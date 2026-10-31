package services;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.java_websocket.*;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.drafts.*;
import org.java_websocket.enums.Opcode;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;

/** HTTP/WebSocket ingress for Cloudflare Tunnel; forwards only to the fixed local TLS port. */
public final class WebSocketBridge extends WebSocketServer implements AutoCloseable {
    private final int backendPort;
    private final ConcurrentHashMap<WebSocket,Backend> backends=new ConcurrentHashMap<>();
    private final ExecutorService io=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"websocket-bridge");t.setDaemon(true);return t;});
    private final CountDownLatch started=new CountDownLatch(1);
    private volatile Exception startupError;
    private static final class BoundedDraft extends Draft_6455 {
        BoundedDraft(){super(Collections.emptyList(),65536);}
        @Override public Draft copyInstance(){return new BoundedDraft();}
        @Override public void processFrame(WebSocketImpl socket,Framedata frame)throws InvalidDataException {
            // This transport sends one complete binary message per TLS chunk. Disallow accumulation of fragments.
            if(!frame.isFin()||frame.getOpcode()==Opcode.CONTINUOUS)throw new InvalidDataException(1008,"Fragmented frames disabled");
            super.processFrame(socket,frame);
        }
    }
    public WebSocketBridge(InetSocketAddress address,int backendPort){super(address,2,List.of(new BoundedDraft()));this.backendPort=backendPort;setConnectionLostTimeout(30);}
    public void awaitStarted()throws Exception{if(!started.await(5,TimeUnit.SECONDS))throw new IOException("WebSocket bridge start timeout");if(startupError!=null)throw startupError;}
    @Override public void onStart(){started.countDown();System.out.println("Cloudflare WebSocket bridge listening on "+getAddress()+" path /play");}
    @Override public void onOpen(WebSocket socket,ClientHandshake handshake) {
        if(!"/play".equals(handshake.getResourceDescriptor())||backends.size()>=64){socket.close(1008,"Invalid path or capacity reached");return;}
        Backend backend=new Backend(socket);backends.put(socket,backend);io.execute(backend::start);
    }
    @Override public void onMessage(WebSocket socket,String text){socket.close(1003,"Binary transport only");}
    @Override public void onMessage(WebSocket socket,ByteBuffer bytes){Backend b=backends.get(socket);if(b==null)return;byte[] data=new byte[bytes.remaining()];bytes.get(data);if(!b.incoming.offer(data)){network.DiagnosticLog.event("bridge_input_queue_full","bridge="+b.id+" queued="+b.incoming.size());b.close();}}
    @Override public void onClose(WebSocket socket,int code,String reason,boolean remote){Backend b=backends.remove(socket);if(b!=null){network.DiagnosticLog.event("bridge_close","bridge="+b.id+" backendPort="+b.tcp.getLocalPort()+" code="+code+" remote="+remote);b.close();}}
    @Override public void onError(WebSocket socket,Exception error){if(socket==null){startupError=error;started.countDown();}else{Backend b=backends.get(socket);if(b!=null){network.DiagnosticLog.event("bridge_error","bridge="+b.id+" error="+network.DiagnosticLog.type(error));b.close();}}}
    private final class Backend {
        final WebSocket websocket;final Socket tcp=new Socket();final AtomicBoolean closed=new AtomicBoolean();
        final String id=UUID.randomUUID().toString();
        final BlockingQueue<byte[]> incoming=new ArrayBlockingQueue<>(64);
        Backend(WebSocket websocket){this.websocket=websocket;}
        void start(){
            try {
                tcp.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),backendPort),3000);tcp.setTcpNoDelay(true);tcp.setSoTimeout(45000);
                network.DiagnosticLog.event("bridge_open","bridge="+id+" backendPort="+tcp.getLocalPort());
                io.execute(()->{
                    try{byte[] buffer=new byte[16384];int count;while(!closed.get()&&(count=tcp.getInputStream().read(buffer))!=-1){
                        if(websocket instanceof WebSocketImpl impl&&impl.outQueue.size()>=64){
                            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
                            while(!closed.get()&&impl.outQueue.size()>=32&&System.nanoTime()<deadline)Thread.sleep(10);
                            if(closed.get())break;
                            if(impl.outQueue.size()>=32){network.DiagnosticLog.event("bridge_output_timeout","bridge="+id+" queued="+impl.outQueue.size());close();break;}
                        }
                        websocket.send(Arrays.copyOf(buffer,count));
                    }}catch(Exception error){network.DiagnosticLog.event("bridge_backend_read_error","bridge="+id+" error="+network.DiagnosticLog.type(error));}finally{close();}
                });
                while(!closed.get()){byte[] data=incoming.poll(45,TimeUnit.SECONDS);if(data==null){network.DiagnosticLog.event("bridge_input_timeout","bridge="+id);break;}tcp.getOutputStream().write(data);}
            }catch(Exception error){network.DiagnosticLog.event("bridge_backend_error","bridge="+id+" error="+network.DiagnosticLog.type(error));}finally{close();}
        }
        void close(){if(!closed.compareAndSet(false,true))return;try{tcp.close();}catch(IOException ignored){}incoming.offer(new byte[0]);websocket.close();}
    }
    @Override public void close(){backends.values().forEach(Backend::close);try{stop(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}io.shutdownNow();}
}
