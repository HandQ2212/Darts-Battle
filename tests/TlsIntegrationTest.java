import network.TransportSecurity;
import protocol.*;
import services.Client;
import services.WebSocketBridge;
import network.WebSocketSocket;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.net.ssl.*;

/** Exercises real encrypted object streams and rejects a different server identity. */
public class TlsIntegrationTest {
    private static int checks;
    private static void check(boolean condition,String text) {
        if(!condition)throw new AssertionError(text);
        checks++;System.out.println("PASS "+text);
    }
    private static class Server implements AutoCloseable {
        final ServerSocket socket;
        final List<Client> clients=Collections.synchronizedList(new ArrayList<>());
        Server(Path store,char[] password) throws Exception {
            socket=TransportSecurity.serverContext(store,password).getServerSocketFactory().createServerSocket(0,50,InetAddress.getLoopbackAddress());
            Thread accept=new Thread(()->{try{while(!socket.isClosed()){Client c=new Client(socket.accept());clients.add(c);c.start();}}catch(IOException ignored){}},"tls-test-server");
            accept.setDaemon(true);accept.start();
        }
        public void close() throws IOException {socket.close();synchronized(clients){clients.forEach(Client::close);}}
    }
    public static void main(String[] args) throws Exception {
        Path store=Path.of(args[0]),cert=Path.of(args[1]),other=Path.of(args[2]);
        char[] password=System.getenv("DARTS_TEST_TLS_PASSWORD").toCharArray();
        SSLContext trust=TransportSecurity.pinnedContext(Files.newInputStream(cert));
        try(Server correct=new Server(store,password);Server wrong=new Server(other,password)) {
            try(Socket socket=TransportSecurity.connect("localhost",correct.socket.getLocalPort(),3000,trust)) {
                check(socket instanceof SSLSocket,"client uses TLS");
                String protocol=((SSLSocket)socket).getSession().getProtocol();
                check(protocol.equals("TLSv1.3")||protocol.equals("TLSv1.2"),"modern TLS negotiated");
                ObjectOutputStream out=new ObjectOutputStream(socket.getOutputStream());out.flush();
                ObjectInputStream in=new ObjectInputStream(socket.getInputStream());in.setObjectInputFilter(WireFilter.create());
                out.writeObject(Message.of(MessageType.PING,null));out.flush();
                check(((Message)in.readObject()).getType()==MessageType.PONG,"encrypted game protocol reaches real server Client");
            }
            try(WebSocketBridge bridge=new WebSocketBridge(new InetSocketAddress("127.0.0.1",0),correct.socket.getLocalPort())) {
                bridge.start();bridge.awaitStarted();
                Socket tunnel=WebSocketSocket.open(URI.create("ws://127.0.0.1:"+bridge.getPort()+"/play"),3000);
                try(Socket encrypted=TransportSecurity.secure(tunnel,"localhost",correct.socket.getLocalPort(),3000,trust)) {
                    check(encrypted instanceof SSLSocket,"end-to-end TLS inside WebSocket");
                    ObjectOutputStream out=new ObjectOutputStream(encrypted.getOutputStream());out.flush();
                    ObjectInputStream in=new ObjectInputStream(encrypted.getInputStream());in.setObjectInputFilter(WireFilter.create());
                    for(int i=0;i<10;i++) {
                        out.writeObject(Message.of(MessageType.PING,null));out.reset();out.flush();
                        check(((Message)in.readObject()).getType()==MessageType.PONG,"WebSocket encrypted ping "+i);
                    }
                }
            }
            boolean rejected=false;
            try(Socket ignored=TransportSecurity.connect("localhost",wrong.socket.getLocalPort(),3000,trust)) {}
            catch(SSLException expected){rejected=true;}
            check(rejected,"wrong certificate rejected without plaintext fallback");
            rejected=false;
            try(Socket plain=new Socket("localhost",correct.socket.getLocalPort())) {
                plain.setSoTimeout(3000);
                ObjectOutputStream out=new ObjectOutputStream(plain.getOutputStream());out.flush();
                try {new ObjectInputStream(plain.getInputStream());}
                catch(IOException expected){rejected=true;}
            }
            check(rejected,"TLS server rejects unencrypted object stream");
            rejected=false;
            try {TransportSecurity.pinnedContext(null);}catch(java.security.cert.CertificateException expected){rejected=true;}
            check(rejected,"missing certificate fails closed");
        } finally {Arrays.fill(password,'\0');}
        System.out.println("ALL "+checks+" TLS CHECKS PASSED");
    }
}
