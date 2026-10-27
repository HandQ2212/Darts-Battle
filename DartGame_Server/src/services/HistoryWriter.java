package services;
import java.util.concurrent.*;
/** Ordered persistence, outside match locks and socket writers. */
public final class HistoryWriter {
    private static final ThreadPoolExecutor WRITER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000), r -> { Thread t=new Thread(r,"history-writer"); t.setDaemon(true); return t; });
    private HistoryWriter() {}
    public static void submit(String matchId, Runnable task) {
        try { WRITER.execute(() -> {
            for(int i=0;i<3;i++) {
                try { task.run(); return; }
                catch(RuntimeException e) {
                    if(i==2) System.err.println("HISTORY_WRITE_FAILED match="+matchId+" "+e.getMessage());
                    else try { Thread.sleep(250); } catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
                }
            }
        }); } catch(RejectedExecutionException e) { System.err.println("HISTORY_QUEUE_FULL match="+matchId); }
    }
    public static void drain() {
        try { WRITER.submit(() -> {}).get(20,TimeUnit.SECONDS); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
}
