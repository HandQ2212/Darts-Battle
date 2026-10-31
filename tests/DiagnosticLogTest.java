import network.DiagnosticLog;
import java.nio.file.*;

public class DiagnosticLogTest {
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("darts-diagnostic-test");
        System.setProperty("darts.logs.dir",dir.toString());
        DiagnosticLog.configure("client");
        DiagnosticLog.event("connection_lost","connection=test-id userId=42 inMatch=true");
        Path file=dir.resolve("client-0.log");long until=System.currentTimeMillis()+5000;
        while(System.currentTimeMillis()<until&&(!Files.exists(file)||!Files.readString(file).contains("connection_lost")))Thread.sleep(25);
        String text=Files.readString(file);
        if(!text.contains("connection=test-id")||!text.contains("userId=42")||!text.contains("event=connection_lost"))throw new AssertionError("diagnostic event missing");
        System.out.println("PASS asynchronous connection diagnostics");
    }
}
