package services;

import protocol.dto.MediaMessages.IceServer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps the long-lived Cloudflare key on the server and returns only expiring ICE credentials. */
final class TurnCredentialService {
    private static final String STUN="stun:stun.cloudflare.com:3478";
    private static final Pattern OBJECT=Pattern.compile("\\{([^{}]*)}");
    private static final Pattern URLS=Pattern.compile("\\\"urls\\\"\\s*:\\s*\\[(.*?)]",Pattern.DOTALL);
    private static final Pattern STRING=Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private final String keyId,apiToken;

    TurnCredentialService() {
        Properties file=new Properties();
        String config=System.getenv().getOrDefault("DARTS_TURN_CONFIG",System.getProperty("darts.turn.config",".local/turn.properties"));
        Path path=Path.of(config);
        try { if(Files.isRegularFile(path))try(var in=Files.newInputStream(path)){file.load(in);} }
        catch(IOException e){System.err.println("TURN configuration could not be read; using STUN only.");}
        keyId=first(System.getenv("DARTS_TURN_KEY_ID"),file.getProperty("turn.keyId"));
        apiToken=first(System.getenv("DARTS_TURN_API_TOKEN"),file.getProperty("turn.apiToken"));
    }

    ArrayList<IceServer> servers() {
        ArrayList<IceServer> result=new ArrayList<>();
        result.add(new IceServer(new ArrayList<>(java.util.List.of(STUN)),"",""));
        if(keyId.isBlank()||apiToken.isBlank())return result;
        HttpURLConnection connection=null;
        try {
            if(!keyId.matches("[A-Za-z0-9_-]{3,128}"))throw new IOException("Invalid TURN key id.");
            URI uri=URI.create("https://rtc.live.cloudflare.com/v1/turn/keys/"+keyId+"/credentials/generate-ice-servers");
            connection=(HttpURLConnection)uri.toURL().openConnection();
            connection.setRequestMethod("POST");connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
            connection.setDoOutput(true);connection.setRequestProperty("Authorization","Bearer "+apiToken);
            connection.setRequestProperty("Content-Type","application/json");
            connection.setRequestProperty("User-Agent","DartsBattle-Server/2.2.0");
            try(var output=connection.getOutputStream()){output.write("{\"ttl\":3600}".getBytes(StandardCharsets.UTF_8));}
            if(connection.getResponseCode()!=201)throw new IOException("TURN credential request failed.");
            String json=new String(connection.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
            Matcher objects=OBJECT.matcher(json);boolean found=false;
            while(objects.find()) {
                String body=objects.group(1);Matcher urlsMatch=URLS.matcher(body);if(!urlsMatch.find())continue;
                ArrayList<String> urls=new ArrayList<>();Matcher strings=STRING.matcher(urlsMatch.group(1));
                while(strings.find())urls.add(unescape(strings.group(1)));
                if(urls.isEmpty())continue;
                String username=field(body,"username"),credential=field(body,"credential");
                if(urls.stream().anyMatch(url->url.startsWith("turn:" )||url.startsWith("turns:"))) {
                    if(username.isBlank()||credential.isBlank())throw new IOException("Incomplete TURN credentials.");
                    result.add(new IceServer(urls,username,credential));found=true;
                }
            }
            if(!found)throw new IOException("TURN servers missing from response.");
        } catch(Exception e) {
            // Never log the response body or API token.
            System.err.println("TURN credentials unavailable ("+e.getClass().getSimpleName()+"); using STUN only.");
        } finally {if(connection!=null)connection.disconnect();}
        return result;
    }

    boolean configured(){return !keyId.isBlank()&&!apiToken.isBlank();}

    private static String field(String body,String name) {
        Matcher m=Pattern.compile("\\\""+Pattern.quote(name)+"\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(body);
        return m.find()?unescape(m.group(1)):"";
    }
    private static String unescape(String value){return value.replace("\\\\","\\").replace("\\\"","\"");}
    private static String first(String a,String b){return a==null||a.isBlank()?(b==null?"":b.trim()):a.trim();}
}
