import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSink;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.MediaMessages.*;
import ui.MediaCallSession;

/** Two actual native peers negotiate through the same signaling messages used by the game. */
public class MediaCallSessionTest {
    private static final String MATCH="media-test",CALL="call-test";
    private static final MediaCallSession[] sessions=new MediaCallSession[2];
    private static final boolean[] joined=new boolean[2];
    private static final CountDownLatch connected=new CountDownLatch(2),ended=new CountDownLatch(2),remoteOff=new CountDownLatch(2);
    private static final AtomicBoolean captured=new AtomicBoolean();
    private static final AtomicReference<String> failure=new AtomicReference<>();
    private static final AtomicReference<String> deviceError=new AtomicReference<>();
    private static final AtomicReference<MediaCallSession.DeviceLists> devices=new AtomicReference<>();
    private static final CountDownLatch devicesReady=new CountDownLatch(1),localFrame=new CountDownLatch(1),remoteFrame=new CountDownLatch(1);
    private static final CountDownLatch secondLocalFrame=new CountDownLatch(1),firstRemoteFrame=new CountDownLatch(1);
    private static final AtomicBoolean ending=new AtomicBoolean();
    private static final AtomicBoolean micEnabled=new AtomicBoolean(),cameraEnabled=new AtomicBoolean();
    private static final AtomicBoolean secondMicEnabled=new AtomicBoolean();
    private static final AtomicBoolean secondCameraEnabled=new AtomicBoolean();
    private static final AtomicInteger micTransitions=new AtomicInteger(),cameraTransitions=new AtomicInteger(),remoteOffEvents=new AtomicInteger();
    private static final AtomicInteger secondMicTransitions=new AtomicInteger();
    private static final AtomicInteger secondCameraTransitions=new AtomicInteger(),firstRemoteOffEvents=new AtomicInteger();
    private static synchronized void send(int index,Message message) {
        switch(message.getType()){
            case MEDIA_JOIN -> {
                joined[index]=true;
                sessions[index].onIceConfig(new IceConfig(MATCH,CALL,new ArrayList<>(),false));
                if(joined[0]&&joined[1]){
                    sessions[0].onPeerState(new PeerState(MATCH,CALL,"B",true,true,"Cả hai đã vào cuộc gọi."));
                    sessions[1].onPeerState(new PeerState(MATCH,CALL,"A",true,false,"Cả hai đã vào cuộc gọi."));
                } else sessions[index].onPeerState(new PeerState(MATCH,CALL,"Đối thủ",false,index==0,"Đang chờ đối thủ tham gia."));
            }
            case MEDIA_DESCRIPTION -> {
                Signal signal=(Signal)message.getPayload();
                if("ANSWER".equals(signal.descriptionType())&&(!sendReceive(signal.value(),"audio")||!sendReceive(signal.value(),"video")))
                    failure.compareAndSet(null,"Answer does not negotiate both media directions");
                sessions[1-index].onDescription(signal);
            }
            case MEDIA_CANDIDATE -> sessions[1-index].onCandidate((Signal)message.getPayload());
            default -> {}
        }
    }
    private static boolean sendReceive(String sdp,String media){
        boolean selected=false;
        for(String line:sdp.split("\\r?\\n")){
            if(line.startsWith("m="))selected=line.startsWith("m="+media+" ");
            else if(selected&&line.equals("a=sendrecv"))return true;
        }
        return false;
    }
    private static MediaCallSession session(int index){
        AtomicBoolean didConnect=new AtomicBoolean(),didEnd=new AtomicBoolean(),didReceiveOff=new AtomicBoolean();
        return new MediaCallSession(message->send(index,message),
            status->{if(status.startsWith("Đã kết nối")&&didConnect.compareAndSet(false,true))connected.countDown();},
            error->{if(index==0)deviceError.set(error);},message->failure.compareAndSet(null,message),
            enabled->{
                if(enabled)captured.set(true);
                if(index==0){micEnabled.set(enabled);micTransitions.incrementAndGet();}
                else {secondMicEnabled.set(enabled);secondMicTransitions.incrementAndGet();}
                if(!enabled&&ending.get()&&didEnd.compareAndSet(false,true))ended.countDown();
            },
            enabled->{if(enabled)captured.set(true);if(index==0){cameraEnabled.set(enabled);cameraTransitions.incrementAndGet();}else{secondCameraEnabled.set(enabled);secondCameraTransitions.incrementAndGet();}},
            image->{if(image!=null){if(index==0)localFrame.countDown();else secondLocalFrame.countDown();}},
            image->{if(image!=null){if(index==1)remoteFrame.countDown();else firstRemoteFrame.countDown();}else{if(index==1)remoteOffEvents.incrementAndGet();else firstRemoteOffEvents.incrementAndGet();if(didReceiveOff.compareAndSet(false,true))remoteOff.countDown();}},
            list->{if(index==0&&devices.compareAndSet(null,list))devicesReady.countDown();});
    }
    private static boolean await(java.util.function.BooleanSupplier condition,long millis)throws InterruptedException {
        long until=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
        while(!condition.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(50);
        return condition.getAsBoolean();
    }
    private static AudioTrack remoteAudio(int index){
        try{var field=MediaCallSession.class.getDeclaredField("remoteAudioTrack");field.setAccessible(true);return (AudioTrack)field.get(sessions[index]);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    public static void main(String[] args)throws Exception {
        boolean physical=Boolean.parseBoolean(System.getenv("DARTS_TEST_PHYSICAL_MEDIA"));
        if(physical)Platform.startup(()->{});
        sessions[0]=session(0);sessions[1]=session(1);
        AudioTrack[] audioTracks=new AudioTrack[2];AudioTrackSink[] sinks=new AudioTrackSink[2];
        try {
            sessions[0].join(MATCH);sessions[1].join(MATCH);
            if(!connected.await(20,TimeUnit.SECONDS))throw new AssertionError("Native peers did not connect: "+failure.get());
            if(failure.get()!=null||captured.get())throw new AssertionError("Unexpected media failure or device capture");
            System.out.println("PASS two media sessions connect without opening mic or camera");
            if(!remoteOff.await(10,TimeUnit.SECONDS))throw new AssertionError("Camera-off state was not exchanged");
            System.out.println("PASS both peers receive camera-off state without server protocol changes");
            if(physical){
                if(!devicesReady.await(10,TimeUnit.SECONDS))throw new AssertionError("Device enumeration timed out");
                MediaCallSession.DeviceLists available=devices.get();
                System.out.println("Device counts: microphones="+available.microphones().size()+" speakers="+available.speakers().size()+" cameras="+available.cameras().size());
                if(!available.microphones().isEmpty()){
                    CountDownLatch audioForFirst=new CountDownLatch(1),audioForSecond=new CountDownLatch(1);
                    sinks[0]=(samples,bits,rate,channels,frames)->audioForFirst.countDown();
                    sinks[1]=(samples,bits,rate,channels,frames)->audioForSecond.countDown();
                    int before=micTransitions.get();sessions[0].setMicrophone(true,-1);
                    if(!await(()->micTransitions.get()>before,10000)||!micEnabled.get())throw new AssertionError("Microphone could not be enabled: "+deviceError.get());
                    if(!await(()->remoteAudio(1)!=null,10000))throw new AssertionError("First microphone remote track missing");
                    audioTracks[1]=remoteAudio(1);audioTracks[1].addSink(sinks[1]);
                    if(!audioForSecond.await(10,TimeUnit.SECONDS))throw new AssertionError("First microphone sent no audio frames");
                    int selected=micTransitions.get();sessions[0].setMicrophone(true,0);
                    if(!await(()->micTransitions.get()>selected,10000)||!micEnabled.get())throw new AssertionError("Microphone selection failed: "+deviceError.get());
                    int reset=micTransitions.get();sessions[0].setMicrophone(true,-1);
                    if(!await(()->micTransitions.get()>reset,10000)||!micEnabled.get())throw new AssertionError("Default microphone selection failed: "+deviceError.get());
                    sessions[0].setMicrophone(false,-1);
                    if(!await(()->!micEnabled.get(),10000))throw new AssertionError("Microphone did not stop");
                    int secondBefore=secondMicTransitions.get();sessions[1].setMicrophone(true,-1);
                    if(!await(()->secondMicTransitions.get()>secondBefore,10000)||!secondMicEnabled.get())throw new AssertionError("Second microphone could not be enabled");
                    if(!await(()->remoteAudio(0)!=null,10000))throw new AssertionError("Second microphone remote track missing");
                    audioTracks[0]=remoteAudio(0);audioTracks[0].addSink(sinks[0]);
                    if(!audioForFirst.await(10,TimeUnit.SECONDS))throw new AssertionError("Second microphone sent no audio frames");
                    sessions[1].setMicrophone(false,-1);
                    if(!await(()->!secondMicEnabled.get(),10000))throw new AssertionError("Second microphone did not stop");
                    audioTracks[0].removeSink(sinks[0]);audioTracks[1].removeSink(sinks[1]);sinks[0]=null;sinks[1]=null;
                    System.out.println("PASS microphone audio reaches both peers; device selection and stop work");
                } else System.out.println("SKIP physical microphone: no device");
                if(!available.speakers().isEmpty()){
                    deviceError.set(null);sessions[0].setSpeaker(0);sessions[0].setSpeaker(-1);Thread.sleep(600);
                    if(deviceError.get()!=null)throw new AssertionError("Speaker selection failed: "+deviceError.get());
                    System.out.println("PASS speaker selection and return to default");
                }
                if(!available.cameras().isEmpty()){
                    int before=cameraTransitions.get();sessions[0].setCamera(true,-1);
                    if(!await(()->cameraTransitions.get()>before,10000)||!cameraEnabled.get())throw new AssertionError("Camera could not be enabled: "+deviceError.get());
                    if(!localFrame.await(12,TimeUnit.SECONDS)||!remoteFrame.await(12,TimeUnit.SECONDS))throw new AssertionError("Camera did not deliver local and remote frames");
                    int change=cameraTransitions.get();sessions[0].selectCamera(available.cameras().size()>1?1:0);
                    if(!await(()->cameraTransitions.get()>change,10000)||!cameraEnabled.get())throw new AssertionError("Camera selection failed: "+deviceError.get());
                    int offBefore=remoteOffEvents.get();sessions[0].setCamera(false,-1);
                    if(!await(()->!cameraEnabled.get()&&remoteOffEvents.get()>offBefore,10000))throw new AssertionError("Camera-off state was not delivered");
                    int reverse=secondCameraTransitions.get();sessions[1].setCamera(true,-1);
                    if(!await(()->secondCameraTransitions.get()>reverse,10000)||!secondCameraEnabled.get())throw new AssertionError("Second camera could not be enabled");
                    if(!secondLocalFrame.await(12,TimeUnit.SECONDS)||!firstRemoteFrame.await(12,TimeUnit.SECONDS))throw new AssertionError("Second camera did not send frames");
                    int reverseOff=firstRemoteOffEvents.get();sessions[1].setCamera(false,-1);
                    if(!await(()->!secondCameraEnabled.get()&&firstRemoteOffEvents.get()>reverseOff,10000))throw new AssertionError("Second camera-off state was not delivered");
                    System.out.println("PASS camera frames flow both ways; device selection and remote off state work");
                } else System.out.println("SKIP physical camera: no device");
            }
            ending.set(true);
            sessions[0].onPeerState(new PeerState(MATCH,CALL,"B",false,false,"Cuộc gọi đã kết thúc."));
            sessions[1].onPeerState(new PeerState(MATCH,CALL,"A",false,false,"Cuộc gọi đã kết thúc."));
            if(!ended.await(10,TimeUnit.SECONDS))throw new AssertionError("Media resources did not close");
            System.out.println("PASS both media sessions release resources on call end");
        } finally {
            for(int i=0;i<2;i++)if(audioTracks[i]!=null&&sinks[i]!=null)try{audioTracks[i].removeSink(sinks[i]);}catch(RuntimeException ignored){}
            sessions[0].close(false);sessions[1].close(false);if(physical)Platform.exit();
        }
    }
}
