import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaType;
import dev.onvoid.webrtc.media.video.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercises the packaged native Windows backend without opening a physical camera or microphone. */
public class NativeWebRtcSmokeTest {
    private static NativeI420Buffer syntheticFrame(){
        NativeI420Buffer buffer=NativeI420Buffer.allocate(320,180);
        for(int i=0;i<buffer.getDataY().capacity();i++)buffer.getDataY().put(i,(byte)96);
        for(int i=0;i<buffer.getDataU().capacity();i++){buffer.getDataU().put(i,(byte)128);buffer.getDataV().put(i,(byte)128);}
        return buffer;
    }
    private static final class CandidateBridge {
        final ConcurrentLinkedQueue<RTCIceCandidate> pending=new ConcurrentLinkedQueue<>();
        final Object lock=new Object();
        volatile RTCPeerConnection destination;
        volatile boolean remoteDescription;
        void send(RTCIceCandidate candidate){synchronized(lock){if(remoteDescription)destination.addIceCandidate(candidate);else pending.add(candidate);}}
        void ready(){synchronized(lock){remoteDescription=true;RTCIceCandidate candidate;while((candidate=pending.poll())!=null)destination.addIceCandidate(candidate);}}
    }
    public static void main(String[] args) throws Exception {
        PeerConnectionFactory factory=new PeerConnectionFactory();
        CustomVideoSource source=new CustomVideoSource();
        VideoTrack track=factory.createVideoTrack("smoke-video",source);
        AtomicInteger frames=new AtomicInteger();CountDownLatch receivedFrame=new CountDownLatch(1);
        CandidateBridge aToB=new CandidateBridge(),bToA=new CandidateBridge();
        RTCPeerConnection[] peers=new RTCPeerConnection[2];
        RTCRtpTransceiver[] transceivers=new RTCRtpTransceiver[2];RTCRtpSender[] senders=new RTCRtpSender[2];
        RTCRtpTransceiver[] remoteTransceiver=new RTCRtpTransceiver[1];RTCRtpReceiver[] receiver=new RTCRtpReceiver[1];VideoTrack[] remoteTrack=new VideoTrack[1];
        VideoTrackSink sink=frame->{frames.incrementAndGet();receivedFrame.countDown();};
        try {
            RTCConfiguration config=new RTCConfiguration();
            peers[0]=factory.createPeerConnection(config,new PeerConnectionObserver(){
                @Override public void onIceCandidate(RTCIceCandidate candidate){aToB.send(candidate);}
            });
            peers[1]=factory.createPeerConnection(config,new PeerConnectionObserver(){
                @Override public void onIceCandidate(RTCIceCandidate candidate){bToA.send(candidate);}
                @Override public void onTrack(RTCRtpTransceiver transceiver){
                    RTCRtpReceiver incoming=transceiver.getReceiver();
                    if(incoming.getTrack() instanceof VideoTrack remote){remoteTransceiver[0]=transceiver;receiver[0]=incoming;remoteTrack[0]=remote;remote.addSink(sink);}
                }
            });
            aToB.destination=peers[1];bToA.destination=peers[0];
            RTCRtpTransceiverInit direction=new RTCRtpTransceiverInit();direction.direction=RTCRtpTransceiverDirection.SEND_RECV;
            transceivers[0]=peers[0].addTransceiver(MediaType.VIDEO,direction);senders[0]=transceivers[0].getSender();senders[0].replaceTrack(track);
            transceivers[1]=peers[1].addTransceiver(MediaType.VIDEO,direction);
            CountDownLatch completed=new CountDownLatch(1);ConcurrentLinkedQueue<String> failures=new ConcurrentLinkedQueue<>();
            peers[0].createOffer(new RTCOfferOptions(),new CreateSessionDescriptionObserver(){
                public void onSuccess(RTCSessionDescription offer){peers[0].setLocalDescription(offer,new SetSessionDescriptionObserver(){
                    public void onSuccess(){peers[1].setRemoteDescription(offer,new SetSessionDescriptionObserver(){
                        public void onSuccess(){aToB.ready();peers[1].createAnswer(new RTCAnswerOptions(),new CreateSessionDescriptionObserver(){
                            public void onSuccess(RTCSessionDescription answer){peers[1].setLocalDescription(answer,new SetSessionDescriptionObserver(){
                                public void onSuccess(){peers[0].setRemoteDescription(answer,new SetSessionDescriptionObserver(){
                                    public void onSuccess(){bToA.ready();completed.countDown();}
                                    public void onFailure(String error){failures.add(error);completed.countDown();}
                                });}
                                public void onFailure(String error){failures.add(error);completed.countDown();}
                            });}
                            public void onFailure(String error){failures.add(error);completed.countDown();}
                        });}
                        public void onFailure(String error){failures.add(error);completed.countDown();}
                    });}
                    public void onFailure(String error){failures.add(error);completed.countDown();}
                });}
                public void onFailure(String error){failures.add(error);completed.countDown();}
            });
            if(!completed.await(12,TimeUnit.SECONDS))throw new AssertionError("SDP offer/answer timed out");
            if(!failures.isEmpty())throw new AssertionError("SDP negotiation failed: "+failures.peek());
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
            while(receivedFrame.getCount()>0&&System.nanoTime()<until){VideoFrame video=new VideoFrame(syntheticFrame(),System.nanoTime());source.pushFrame(video);video.release();Thread.sleep(67);}
            if(!receivedFrame.await(1,TimeUnit.SECONDS))throw new AssertionError("Native peer connection did not deliver a video frame");
            System.out.println("PASS Windows native WebRTC offer/answer and synthetic video frame ("+frames.get()+" frames)");
        } finally {
            if(senders[0]!=null)try{senders[0].replaceTrack(null);}catch(RuntimeException ignored){}
            for(RTCPeerConnection peer:peers)if(peer!=null)try{peer.close();}catch(RuntimeException ignored){}
            for(RTCRtpSender sender:senders)if(sender!=null)try{sender.dispose();}catch(RuntimeException ignored){}
            for(RTCRtpTransceiver transceiver:transceivers)if(transceiver!=null)try{transceiver.dispose();}catch(RuntimeException ignored){}
            if(remoteTrack[0]!=null){try{remoteTrack[0].removeSink(sink);remoteTrack[0].dispose();}catch(RuntimeException ignored){}}
            if(receiver[0]!=null)try{receiver[0].dispose();}catch(RuntimeException ignored){}
            if(remoteTransceiver[0]!=null)try{remoteTransceiver[0].dispose();}catch(RuntimeException ignored){}
            try{track.dispose();}catch(RuntimeException ignored){}
            try{source.dispose();}catch(RuntimeException ignored){}
            factory.dispose();
        }
    }
}
