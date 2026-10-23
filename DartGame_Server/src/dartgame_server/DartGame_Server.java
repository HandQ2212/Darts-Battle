package dartgame_server;
import services.*;
import network.TransportSecurity;
import java.nio.file.Path;
import java.util.Arrays;
import database.DBConnection;
import java.net.*;
import java.util.concurrent.*;
public class DartGame_Server {
    public static void main(String[] args) throws Exception {
        DBConnection.getInstance().initialize();
        int port=Integer.parseInt(System.getProperty("darts.port",System.getenv().getOrDefault("DARTS_PORT","8080")));
        String host=System.getProperty("darts.bind",System.getenv().getOrDefault("DARTS_BIND","0.0.0.0"));
        ScheduledExecutorService maintenance=Executors.newSingleThreadScheduledExecutor();
        maintenance.scheduleAtFixedRate(()->{
            try { LobbyService.getInstance().expire(); GameService.getInstance().expireTurns(); }
            catch(RuntimeException e) { System.err.println("Maintenance failed: "+e); }
        },1,1,TimeUnit.SECONDS);
        String keyStore = System.getProperty("darts.tls.keystore", System.getenv().getOrDefault("DARTS_TLS_KEYSTORE", ""));
        ServerSocket listener;
        try {
            if (keyStore.isBlank()) listener = new ServerSocket();
            else {
                char[] password = System.getenv().getOrDefault("DARTS_TLS_PASSWORD", "").toCharArray();
                try { listener = TransportSecurity.serverContext(Path.of(keyStore), password).getServerSocketFactory().createServerSocket(); }
                finally { Arrays.fill(password, '\0'); }
            }
        } catch (Exception e) { maintenance.shutdownNow(); throw e; }
        WebSocketBridge bridge=null;
        try(ServerSocket server=listener) {
            server.bind(new InetSocketAddress(host,port));
            System.out.println("Darts Battle server listening on "+host+":"+server.getLocalPort()+ (keyStore.isBlank()?" (development TCP)":" (TLS)"));
            String wsPort=System.getenv().getOrDefault("DARTS_WEBSOCKET_PORT", "");
            if(!wsPort.isBlank()) {
                if(keyStore.isBlank())throw new IllegalStateException("WebSocket ingress requires the TLS game listener");
                bridge=new WebSocketBridge(new InetSocketAddress("127.0.0.1",Integer.parseInt(wsPort)),server.getLocalPort());
                bridge.start();bridge.awaitStarted();
            }
            WebSocketBridge activeBridge=bridge;
            Runtime.getRuntime().addShutdownHook(new Thread(()->{ if(activeBridge!=null)activeBridge.close(); HistoryWriter.drain(); maintenance.shutdownNow(); }));
            while(true) new Client(server.accept()).start();
        } finally { if(bridge!=null)bridge.close(); maintenance.shutdownNow(); }
    }
}
