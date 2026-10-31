import network.*;
import services.WebSocketBridge;
import protocol.*;
import protocol.dto.HistoryMessages.*;
import javax.net.ssl.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.*;

/** Real TLS/WebSocket regression using a full history page and a deliberately slow reader. */
public class HistoryTransportIntegrationTest {
    public static void main(String[] args)throws Exception {
        SSLContext trust=TransportSecurity.pinnedContext(Files.newInputStream(Path.of(args[1])));
        SSLContext identity=TransportSecurity.serverContext(Path.of(args[0]),System.getenv("DARTS_TEST_TLS_PASSWORD").toCharArray());
        ArrayList<Match> matches=new ArrayList<>();
        for(int i=0;i<25;i++)matches.add(new Match("match-"+i,"PVP",1,"Player One",2L,"Player Two",1L,"HUMAN","ZERO_SCORE","2026-01-01 12:00:00",0,63,null,false,false));
        Page page=new Page(1,0,true,matches,null);
        ArrayList<Dart> darts=new ArrayList<>();
        for(int i=0;i<300;i++)darts.add(new Dart(i/3+1,i%3+1,i%2==0?1L:2L,"HUMAN",20,201,i%7==0));
        Detail detail=new Detail(matches.get(0),darts,null);
        try(ServerSocket listener=identity.getServerSocketFactory().createServerSocket(0,50,InetAddress.getLoopbackAddress());
            WebSocketBridge bridge=new WebSocketBridge(new InetSocketAddress("127.0.0.1",0),listener.getLocalPort())){
            bridge.start();bridge.awaitStarted();
            ExecutorService worker=Executors.newSingleThreadExecutor();
            try{
                Future<?> serving=worker.submit(()->{try{
                    for(int mode=0;mode<2;mode++)try(Socket socket=listener.accept()){
                        socket.setSoTimeout(10000);
                        OutputStream stream=socket.getOutputStream();
                        // Unbuffered mode reproduces the old server's burst of tiny TLS records.
                        ObjectOutputStream out=new ObjectOutputStream(mode==0?stream:new BufferedOutputStream(stream,16384));out.flush();
                        ObjectInputStream in=new ObjectInputStream(socket.getInputStream());
                        for(int i=0;i<100;i++){in.readObject();out.writeObject(Message.of(MessageType.PLAYER_MATCHES_RESULT,page));out.reset();out.flush();
                            in.readObject();out.writeObject(Message.of(MessageType.MATCH_DETAIL_RESULT,detail));out.reset();out.flush();}
                        in.readObject();out.writeObject(Message.of(MessageType.PONG,null));out.flush();
                    }
                }catch(Exception e){throw new RuntimeException(e);}});
                for(int mode=0;mode<2;mode++){
                    Socket transport=WebSocketSocket.open(URI.create("ws://127.0.0.1:"+bridge.getPort()+"/play"),5000);
                    try(Socket socket=TransportSecurity.secure(transport,"localhost",443,5000,trust)){
                        socket.setSoTimeout(10000);
                        ObjectOutputStream out=new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream(),16384));out.flush();
                        ObjectInputStream in=new ObjectInputStream(socket.getInputStream());in.setObjectInputFilter(WireFilter.create());
                        for(int i=0;i<100;i++){
                            out.writeObject(Message.of(MessageType.PLAYER_MATCHES_REQUEST,new PageRequest(1,0,false)));out.reset();out.flush();
                            Thread.sleep(5);Page received=(Page)((Message)in.readObject()).getPayload();
                            if(!received.equals(page))throw new AssertionError("history changed in transit");
                            out.writeObject(Message.of(MessageType.MATCH_DETAIL_REQUEST,new DetailRequest("match-0")));out.reset();out.flush();
                            Thread.sleep(5);Detail receivedDetail=(Detail)((Message)in.readObject()).getPayload();
                            if(!receivedDetail.equals(detail))throw new AssertionError("detail changed in transit");
                        }
                        out.writeObject(Message.of(MessageType.PING,null));out.flush();
                        if(((Message)in.readObject()).getType()!=MessageType.PONG)throw new AssertionError("connection unusable after history");
                    }
                    System.out.println("PASS 100 history pages and details; slow reader; buffered="+(mode==1));
                }
                serving.get(30,TimeUnit.SECONDS);
            }finally{worker.shutdownNow();}
        }
    }
}
