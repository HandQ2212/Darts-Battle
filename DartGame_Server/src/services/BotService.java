package services;
import java.util.*;
import java.util.concurrent.*;
public final class BotService {
    private static final BotService INSTANCE=new BotService();
    private final ScheduledExecutorService timer=Executors.newScheduledThreadPool(2,r->{Thread t=new Thread(r,"bot-timer");t.setDaemon(true);return t;});
    private final ConcurrentHashMap<String,List<ScheduledFuture<?>>> tasks=new ConcurrentHashMap<>();
    private BotService() {}
    public static BotService getInstance() { return INSTANCE; }
    public void playTurn(String matchId,long turnId) {
        cancel(matchId); List<ScheduledFuture<?>> list=new ArrayList<>(); tasks.put(matchId,list);
        int angle=ThreadLocalRandom.current().nextInt(360);
        long delay=Long.getLong("darts.botDelayMillis",700L);
        for(int i=0;i<=3;i++) {
            final int dart=i;
            list.add(timer.schedule(()->{
                try { GameService.getInstance().playBotStep(matchId,turnId,dart,angle,
                        ThreadLocalRandom.current().nextDouble(-140,140),ThreadLocalRandom.current().nextDouble(-140,140)); }
                catch(RuntimeException e) { System.err.println("Bot failed: "+e.getMessage()); }
                if(dart==3) tasks.remove(matchId,list);
            },delay*(i+1),TimeUnit.MILLISECONDS));
        }
    }
    public void cancel(String matchId) { List<ScheduledFuture<?>> list=tasks.remove(matchId); if(list!=null) list.forEach(f->f.cancel(false)); }
}
