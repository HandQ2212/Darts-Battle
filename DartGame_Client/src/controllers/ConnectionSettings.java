package controllers;
import java.io.*;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.net.ssl.SSLContext;
import network.TransportSecurity;

/** Endpoint is configured by the owner at build time, never requested from players. */
public final class ConnectionSettings {
    private static final Properties PROFILE = new Properties();
    private static final Preferences PREFS = Preferences.userNodeForPackage(ConnectionSettings.class);
    static {
        try (InputStream in = ConnectionSettings.class.getResourceAsStream("/server.properties")) {
            if (in != null) PROFILE.load(in);
        } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    private ConnectionSettings() {}
    public static String host() { return System.getProperty("darts.host", PROFILE.getProperty("server.host", "")).trim(); }
    public static int port() { return Integer.parseInt(System.getProperty("darts.port", PROFILE.getProperty("server.port", "8443"))); }
    public static String transport(){return PROFILE.getProperty("server.transport","tcp");}
    public static java.net.URI websocketUri(){return java.net.URI.create("wss://"+host()+":"+port()+"/play");}
    public static boolean tls() { return Boolean.parseBoolean(PROFILE.getProperty("server.tls", "true")); }
    public static SSLContext tlsContext() throws Exception {
        return tls() ? TransportSecurity.pinnedContext(ConnectionSettings.class.getResourceAsStream("/server-cert.pem")) : null;
    }
    public static boolean configured() {
        try { return !host().isBlank() && port() > 0 && port() <= 65535; }
        catch (NumberFormatException e) { return false; }
    }
    public static String lastUsername() { return PREFS.get("username", ""); }
    public static void rememberUsername(String name) { PREFS.put("username", name); }
}
