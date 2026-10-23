import network.*;
import protocol.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.net.ssl.*;

/** Owner diagnostic: verifies public WSS routing and pinned inner TLS, without logging in. */
public class InternetConnectionProbe {
    public static void main(String[] args)throws Exception {
        URI endpoint=URI.create(args[0]);
        SSLContext context=TransportSecurity.pinnedContext(Files.newInputStream(Path.of(args[1])));
        Socket tunnel=WebSocketSocket.open(endpoint,8000);
        try(Socket socket=TransportSecurity.secure(tunnel,endpoint.getHost(),endpoint.getPort()>0?endpoint.getPort():443,8000,context)) {
            ObjectOutputStream out=new ObjectOutputStream(socket.getOutputStream());out.flush();
            ObjectInputStream in=new ObjectInputStream(socket.getInputStream());in.setObjectInputFilter(WireFilter.create());
            out.writeObject(Message.of(MessageType.PING,null));out.flush();
            if(((Message)in.readObject()).getType()!=MessageType.PONG)throw new AssertionError("Wrong game response");
            System.out.println("PASS "+endpoint+" — pinned "+((SSLSocket)socket).getSession().getProtocol()+", game PONG received");
        }
    }
}
