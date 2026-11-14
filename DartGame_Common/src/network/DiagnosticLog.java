package network;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.concurrent.*;
import java.util.logging.*;

/** Bounded, asynchronous diagnostics; never pass payloads or credentials. */
public final class DiagnosticLog {
    private static final ThreadPoolExecutor QUEUE=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(512),r->{Thread t=new Thread(r,"diagnostics");t.setDaemon(true);return t;},new ThreadPoolExecutor.DiscardPolicy());
    private static volatile Logger logger;
    private DiagnosticLog(){}
    public static synchronized void configure(String role){
        if(logger!=null)return;
        try{
            String configured=System.getProperty("darts.logs.dir");
            Path dir=configured!=null?Path.of(configured):"server".equals(role)?Path.of(".local","logs"):Path.of(System.getProperty("user.home"),"DartsBattle","logs");
            Files.createDirectories(dir);
            Logger target=Logger.getLogger("darts.diagnostics."+role);target.setUseParentHandlers(false);
            FileHandler handler=new FileHandler(dir.resolve(role+"-%g.log").toString(),1024*1024,4,true);
            handler.setFormatter(new java.util.logging.Formatter(){public String format(LogRecord r){return r.getMessage()+System.lineSeparator();}});
            target.addHandler(handler);logger=target;
        }catch(Exception ignored){}
    }
    public static void event(String event,String fields){
        Logger target=logger;if(target==null)return;
        String line=OffsetDateTime.now()+" event="+event+" "+fields;
        QUEUE.execute(()->{try{target.info(line);}catch(RuntimeException ignored){}});
    }
    public static String type(Throwable error){return error==null?"none":error.getClass().getSimpleName();}
}
