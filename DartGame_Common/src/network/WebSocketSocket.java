package network;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.*;

/** Bounded binary WebSocket byte stream. Can be wrapped by SSLSocket for end-to-end TLS. */
public final class WebSocketSocket extends Socket implements WebSocket.Listener {
    private final BlockingQueue<byte[]> incoming=new ArrayBlockingQueue<>(64);
    private volatile boolean closed;
    private volatile int timeout=5000;
    private volatile WebSocket websocket;
    private ByteArrayInputStream part=new ByteArrayInputStream(new byte[0]);
    private final InputStream input=new InputStream() {
        @Override public int read() throws IOException {byte[] b=new byte[1];return read(b,0,1)==-1?-1:b[0]&255;}
        @Override public int read(byte[] data,int offset,int length) throws IOException {
            java.util.Objects.checkFromIndexSize(offset,length,data.length);
            if(length==0)return 0;
            while(part.available()==0) {
                if(closed&&incoming.isEmpty())return -1;
                try {
                    byte[] bytes=incoming.poll(timeout>0?timeout:35000,TimeUnit.MILLISECONDS);
                    if(bytes==null){if(closed)return -1;throw new SocketTimeoutException("WebSocket read timed out");}
                    part=new ByteArrayInputStream(bytes);
                    // Replenish receive demand only after freeing a queue slot.
                    WebSocket ws=websocket;if(!closed&&ws!=null)ws.request(1);
                } catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}
            }
            return part.read(data,offset,length);
        }
        @Override public int available(){return part.available();}
    };
    private final OutputStream output=new OutputStream() {
        @Override public void write(int value)throws IOException{write(new byte[]{(byte)value});}
        @Override public synchronized void write(byte[] data,int offset,int length)throws IOException {
            java.util.Objects.checkFromIndexSize(offset,length,data.length);
            if(closed)throw new SocketException("WebSocket closed");
            while(length>0) {
                int count=Math.min(length,16384);
                byte[] copy=java.util.Arrays.copyOfRange(data,offset,offset+count);
                try{websocket.sendBinary(ByteBuffer.wrap(copy),true).get(8,TimeUnit.SECONDS);}
                catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}
                catch(ExecutionException|TimeoutException e){close();throw new IOException("WebSocket write failed",e);}
                offset+=count;length-=count;
            }
        }
    };
    public static WebSocketSocket open(URI uri,int timeout)throws IOException {
        if(!"wss".equals(uri.getScheme())&&!"ws".equals(uri.getScheme()))throw new IOException("Invalid WebSocket endpoint");
        WebSocketSocket result=new WebSocketSocket();result.timeout=timeout;
        try {
            result.websocket=HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).build()
                    .newWebSocketBuilder().connectTimeout(Duration.ofMillis(timeout)).buildAsync(uri,result).get(timeout+1000L,TimeUnit.MILLISECONDS);
            return result;
        } catch(InterruptedException e){Thread.currentThread().interrupt();result.close();throw new InterruptedIOException();}
        catch(ExecutionException|TimeoutException e){result.close();throw new IOException("WebSocket connection failed",e);}
    }
    @Override public void onOpen(WebSocket ws){websocket=ws;ws.request(1);}
    @Override public CompletionStage<?> onBinary(WebSocket ws,ByteBuffer data,boolean last) {
        if(data.remaining()>65536){DiagnosticLog.event("websocket_frame_limit","bytes="+data.remaining());close();return null;}
        byte[] bytes=new byte[data.remaining()];data.get(bytes);
        if(!closed&&!incoming.offer(bytes)){DiagnosticLog.event("websocket_queue_full","queued="+incoming.size());close();}return null;
    }
    @Override public CompletionStage<?> onText(WebSocket ws,CharSequence data,boolean last){close();return null;}
    @Override public CompletionStage<?> onClose(WebSocket ws,int status,String reason){DiagnosticLog.event("websocket_close","code="+status);closed=true;incoming.offer(new byte[0]);return null;}
    @Override public void onError(WebSocket ws,Throwable error){DiagnosticLog.event("websocket_error","error="+DiagnosticLog.type(error));closed=true;incoming.offer(new byte[0]);}
    @Override public InputStream getInputStream(){return input;}
    @Override public OutputStream getOutputStream(){return output;}
    @Override public void setSoTimeout(int millis){if(millis<0)throw new IllegalArgumentException();timeout=millis;}
    @Override public int getSoTimeout(){return timeout;}
    @Override public void setTcpNoDelay(boolean enabled){}
    @Override public boolean getTcpNoDelay(){return true;}
    @Override public boolean isConnected(){return websocket!=null&&!closed;}
    @Override public boolean isClosed(){return closed;}
    @Override public InetAddress getInetAddress(){return InetAddress.getLoopbackAddress();}
    @Override public InetAddress getLocalAddress(){return InetAddress.getLoopbackAddress();}
    @Override public synchronized void close(){closed=true;incoming.offer(new byte[0]);WebSocket ws=websocket;if(ws!=null)ws.abort();}
    @Override public void shutdownInput(){close();}
    @Override public void shutdownOutput(){close();}
}
