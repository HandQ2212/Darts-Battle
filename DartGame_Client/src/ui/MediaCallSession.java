package ui;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.*;
import dev.onvoid.webrtc.media.audio.*;
import dev.onvoid.webrtc.media.video.*;
import javafx.application.Platform;
import javafx.scene.image.*;
import protocol.Message;
import protocol.MessageType;
import protocol.dto.MediaMessages.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Per-match WebRTC resources. All peer-connection and device changes run on one worker. */
public final class MediaCallSession implements AutoCloseable {
    public record DeviceLists(List<String> microphones,List<String> speakers,List<String> cameras) {}
    private final Consumer<Message> send;
    private final Consumer<String> status;
    private final Consumer<String> error;
    private final Consumer<Boolean> micState,cameraState;
    private final Consumer<Image> localFrame,remoteFrame;
    private final Consumer<DeviceLists> deviceLists;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"darts-webrtc");t.setDaemon(true);return t;});
    private final ArrayList<Signal> pendingCandidates=new ArrayList<>();
    private final AtomicBoolean localFrameQueued=new AtomicBoolean(),remoteFrameQueued=new AtomicBoolean();
    private volatile boolean closed;
    private String matchId,callId,lastEndedCallId;
    private boolean joined,peerJoined,offerer,remoteDescriptionSet,microphoneEnabled,cameraEnabled,turnAvailable,devicesEnumerated;
    private IceConfig iceConfig;
    private PeerConnectionFactory factory;
    private AudioDeviceModule audioModule;
    private volatile RTCPeerConnection peer;
    private RTCRtpTransceiver audioTransceiver,videoTransceiver,remoteAudioTransceiver,remoteVideoTransceiver;
    private RTCRtpReceiver remoteAudioReceiver,remoteVideoReceiver;
    private RTCRtpSender audioSender,videoSender;
    private AudioTrackSource audioSource;
    private AudioTrack audioTrack;
    private VideoDeviceSource videoSource;
    private VideoTrack videoTrack;
    private volatile VideoTrack remoteVideoTrack;
    private volatile AudioTrack remoteAudioTrack;
    private final VideoTrackSink localSink,remoteSink;
    private final ArrayList<String> microphoneNames=new ArrayList<>(),speakerNames=new ArrayList<>();
    private final ArrayList<AudioDevice> microphones=new ArrayList<>(),speakers=new ArrayList<>();
    private final ArrayList<VideoDevice> cameras=new ArrayList<>();

    public MediaCallSession(Consumer<Message> send,Consumer<String> status,Consumer<String> error,
            Consumer<Boolean> micState,Consumer<Boolean> cameraState,Consumer<Image> localFrame,
            Consumer<Image> remoteFrame,Consumer<DeviceLists> deviceLists) {
        this.send=send;this.status=status;this.error=error;this.micState=micState;this.cameraState=cameraState;
        this.localFrame=localFrame;this.remoteFrame=remoteFrame;this.deviceLists=deviceLists;
        this.localSink=frame->renderFrame(frame,this.localFrame,localFrameQueued);
        this.remoteSink=frame->renderFrame(frame,this.remoteFrame,remoteFrameQueued);
    }

    public void join(String id) { post(()->{matchId=id;joined=true;status.accept("Đang kết nối cuộc gọi…");send.accept(Message.of(MessageType.MEDIA_JOIN,new JoinRequest(id)));}); }
    public void onPeerState(PeerState value) { post(()->{
        if(!Objects.equals(matchId,value.matchId()))return;
        boolean endedNotice=value.notice()!=null&&(value.notice().startsWith("Cuộc gọi đã kết thúc")||value.notice().startsWith("Trận đã kết thúc"));
        if(endedNotice&&joined&&value.callId()!=null&&value.callId().equals(lastEndedCallId))return;
        if(endedNotice&&callId!=null&&!Objects.equals(callId,value.callId()))return;
        callId=value.callId();offerer=value.offerer();
        if(endedNotice){lastEndedCallId=callId;finishRemote(value.notice());return;}
        peerJoined=value.peerJoined();status.accept(value.notice()==null?"":value.notice());
        if(peerJoined){ensurePeer();negotiateIfReady();}
    }); }
    public void onIceConfig(IceConfig value) { post(()->{
        if(!Objects.equals(matchId,value.matchId()))return;
        callId=value.callId();boolean refresh=iceConfig!=null;iceConfig=value;turnAvailable=value.turnAvailable();ensurePeer();negotiateIfReady();
        if(!turnAvailable)status.accept("Đang gọi trực tiếp · chưa có kết nối dự phòng TURN");
        if(refresh&&peer!=null)updateIceServers(value);
        if(!devicesEnumerated){enumerateDevices();devicesEnumerated=true;}
    }); }
    public void onDescription(Signal signal) { post(()->{
        if(!valid(signal))return;
        RTCSdpType type="OFFER".equals(signal.descriptionType())?RTCSdpType.OFFER:RTCSdpType.ANSWER;
        peer.setRemoteDescription(new RTCSessionDescription(type,signal.value()),new SetSessionDescriptionObserver(){
            public void onSuccess(){post(()->{remoteDescriptionSet=true;flushCandidates();if(type==RTCSdpType.OFFER)createAnswer();});}
            public void onFailure(String message){post(()->error.accept("Không thể thiết lập kết nối cuộc gọi."));}
        });
    }); }
    public void onCandidate(Signal signal) { post(()->{
        if(!valid(signal)||signal.mid()==null||signal.mLineIndex()<0||signal.mLineIndex()>31)return;
        if(!remoteDescriptionSet){if(pendingCandidates.size()<256)pendingCandidates.add(signal);return;}
        addCandidate(signal);
    }); }
    private boolean valid(Signal value){return joined&&peerJoined&&peer!=null&&value!=null&&Objects.equals(matchId,value.matchId())&&Objects.equals(callId,value.callId());}
    private void ensurePeer(){
        if(factory==null)initializeFactory();
        if(peer!=null||!peerJoined||iceConfig==null)return;
        if(factory==null)return;
        RTCConfiguration settings=new RTCConfiguration();
        for(IceServer source:iceConfig.servers()) {RTCIceServer server=new RTCIceServer();server.urls=new ArrayList<>(source.urls());server.username=source.username();server.password=source.credential();settings.iceServers.add(server);}
        peer=factory.createPeerConnection(settings,new PeerConnectionObserver(){
            @Override public void onIceCandidate(RTCIceCandidate candidate){
                if(candidate==null||candidate.sdp==null||candidate.sdp.length()>2048)return;
                post(()->send.accept(Message.of(MessageType.MEDIA_CANDIDATE,new Signal(matchId,callId,"ICE",candidate.sdp,candidate.sdpMid,candidate.sdpMLineIndex,0))));
            }
            @Override public void onConnectionChange(RTCPeerConnectionState state){post(()->{String message=switch(state){case CONNECTED->"Đã kết nối mic/video";case CONNECTING->"Đang thiết lập kết nối…";case FAILED->"Không kết nối được. Hãy thử rời và tham gia lại.";case DISCONNECTED->"Kết nối bị gián đoạn";case CLOSED->"Cuộc gọi đã đóng";default->"Đang chờ kết nối…";};if(!turnAvailable)message+=" · chưa có kết nối dự phòng TURN";status.accept(message);});}
            @Override public void onTrack(RTCRtpTransceiver transceiver){
                RTCRtpReceiver receiver=transceiver.getReceiver();var track=receiver.getTrack();
                if(track instanceof AudioTrack remoteAudio){remoteAudio.setVolume(1.0);post(()->{remoteAudioTransceiver=transceiver;remoteAudioReceiver=receiver;remoteAudioTrack=remoteAudio;});}
                if(track instanceof VideoTrack remoteVideo){post(()->{remoteVideoTransceiver=transceiver;remoteVideoReceiver=receiver;attachRemoteVideo(remoteVideo);});}
            }
        });
        peer.setAudioRecording(false);peer.setAudioPlayout(true);
        RTCRtpTransceiverInit direction=new RTCRtpTransceiverInit();direction.direction=RTCRtpTransceiverDirection.SEND_RECV;direction.streamIds=new ArrayList<>(List.of("darts-match"));
        audioTransceiver=peer.addTransceiver(MediaType.AUDIO,direction);audioSender=audioTransceiver.getSender();
        RTCRtpTransceiverInit videoDirection=new RTCRtpTransceiverInit();videoDirection.direction=RTCRtpTransceiverDirection.SEND_RECV;videoDirection.streamIds=new ArrayList<>(List.of("darts-match"));
        videoTransceiver=peer.addTransceiver(MediaType.VIDEO,videoDirection);videoSender=videoTransceiver.getSender();
        status.accept("Đang thiết lập kết nối…");
    }
    private void updateIceServers(IceConfig config){
        try {
            RTCConfiguration settings=new RTCConfiguration();
            for(IceServer source:config.servers()){RTCIceServer server=new RTCIceServer();server.urls=new ArrayList<>(source.urls());server.username=source.username();server.password=source.credential();settings.iceServers.add(server);}
            peer.setConfiguration(settings);
            if(offerer){RTCOfferOptions options=new RTCOfferOptions();options.iceRestart=true;createOffer(options);}
        }catch(RuntimeException e){status.accept("Không thể làm mới kết nối dự phòng TURN.");}
    }
    private void initializeFactory(){
        try {
            audioModule=new AudioDeviceModule();factory=new PeerConnectionFactory(audioModule);
            audioModule.setMicrophoneMute(true);
        } catch(Throwable e){
            if(audioModule!=null)try{audioModule.dispose();}catch(RuntimeException ignored){}
            audioModule=null;
            try{factory=new PeerConnectionFactory();error.accept("Đang dùng thiết bị âm thanh hệ thống mặc định.");}
            catch(Throwable fallback){factory=null;error.accept("Không khởi tạo được WebRTC: "+safeMessage(fallback));}
        }
    }
    private void negotiateIfReady(){
        if(!joined||!peerJoined||peer==null)return;
        if(offerer&&peer.getLocalDescription()==null&&peer.getRemoteDescription()==null)createOffer();
    }
    private void createOffer(){createOffer(new RTCOfferOptions());}
    private void createOffer(RTCOfferOptions options){
        peer.createOffer(options,new CreateSessionDescriptionObserver(){
            public void onSuccess(RTCSessionDescription description){peer.setLocalDescription(description,new SetSessionDescriptionObserver(){
                public void onSuccess(){post(()->sendDescription("OFFER",peer.getLocalDescription().sdp));}
                public void onFailure(String message){post(()->error.accept("Không thể tạo lời mời cuộc gọi."));}
            });}
            public void onFailure(String message){post(()->error.accept("Không thể tạo lời mời cuộc gọi."));}
        });
    }
    private void createAnswer(){
        peer.createAnswer(new RTCAnswerOptions(),new CreateSessionDescriptionObserver(){
            public void onSuccess(RTCSessionDescription description){peer.setLocalDescription(description,new SetSessionDescriptionObserver(){
                public void onSuccess(){post(()->sendDescription("ANSWER",peer.getLocalDescription().sdp));}
                public void onFailure(String message){post(()->error.accept("Không thể chấp nhận cuộc gọi."));}
            });}
            public void onFailure(String message){post(()->error.accept("Không thể chấp nhận cuộc gọi."));}
        });
    }
    private void sendDescription(String type,String sdp){if(sdp!=null&&sdp.length()<=65536)send.accept(Message.of(MessageType.MEDIA_DESCRIPTION,new Signal(matchId,callId,type,sdp,null,0,0)));else error.accept("Dữ liệu kết nối quá lớn.");}
    private void addCandidate(Signal value){peer.addIceCandidate(new RTCIceCandidate(value.mid(),value.mLineIndex(),value.value()));}
    private void flushCandidates(){for(Signal item:new ArrayList<>(pendingCandidates))addCandidate(item);pendingCandidates.clear();}
    private void enumerateDevices(){
        try{microphones.clear();microphones.addAll(audioModule==null?MediaDevices.getAudioCaptureDevices():audioModule.getRecordingDevices());microphoneNames.clear();microphones.forEach(d->microphoneNames.add(d.getName()));}
        catch(RuntimeException e){error.accept("Không thể liệt kê micro: "+safeMessage(e));}
        try{speakers.clear();speakers.addAll(audioModule==null?MediaDevices.getAudioRenderDevices():audioModule.getPlayoutDevices());speakerNames.clear();speakers.forEach(d->speakerNames.add(d.getName()));}
        catch(RuntimeException e){error.accept("Không thể liệt kê loa: "+safeMessage(e));}
        try{cameras.clear();cameras.addAll(MediaDevices.getVideoCaptureDevices());}
        catch(RuntimeException e){error.accept("Không thể liệt kê camera: "+safeMessage(e));}
        deviceLists.accept(new DeviceLists(new ArrayList<>(microphoneNames),new ArrayList<>(speakerNames),cameras.stream().map(Device::getName).toList()));
    }
    private int audioInputDeviceIndex=-1;
    public void setMicrophone(boolean enabled,int deviceIndex){post(()->{
        if(!ready())return;
        try {
            if(enabled&&audioTrack==null){
                if(audioModule!=null&&deviceIndex>=0&&deviceIndex<microphones.size())audioModule.setRecordingDevice(microphones.get(deviceIndex));
                audioInputDeviceIndex=deviceIndex;
                AudioOptions options=new AudioOptions();options.echoCancellation=true;options.autoGainControl=true;options.noiseSuppression=true;options.highpassFilter=true;
                audioSource=factory.createAudioSource(options);audioTrack=factory.createAudioTrack("darts-audio",audioSource);audioTrack.setEnabled(true);audioSender.replaceTrack(audioTrack);
            }
            else if(enabled&&audioModule!=null&&deviceIndex>=0&&deviceIndex<microphones.size()&&deviceIndex!=audioInputDeviceIndex){peer.setAudioRecording(false);audioModule.setRecordingDevice(microphones.get(deviceIndex));audioInputDeviceIndex=deviceIndex;peer.setAudioRecording(true);}
            microphoneEnabled=enabled;
            if(audioTrack!=null)audioTrack.setEnabled(enabled);
            if(audioModule!=null)audioModule.setMicrophoneMute(!enabled);peer.setAudioRecording(enabled);micState.accept(enabled);
        }catch(RuntimeException e){microphoneEnabled=false;if(audioTrack!=null)audioTrack.setEnabled(false);if(audioModule!=null)try{audioModule.setMicrophoneMute(true);}catch(RuntimeException ignored){}try{peer.setAudioRecording(false);}catch(RuntimeException ignored){}micState.accept(false);error.accept("Không mở được micro: "+safeMessage(e));}
    });}
    public void setSpeaker(int deviceIndex){post(()->{if(deviceIndex>=0&&deviceIndex<speakers.size())try{if(audioModule!=null)audioModule.setPlayoutDevice(speakers.get(deviceIndex));else if(factory!=null)MediaDevices.getDefaultAudioRenderDevice();}catch(RuntimeException e){error.accept("Không đổi được loa: "+safeMessage(e));}});}
    public void setCamera(boolean enabled,int deviceIndex){post(()->{int selectedDevice=deviceIndex;
        if(!ready())return;
        try {
            if(!enabled){if(videoSource!=null)videoSource.stop();if(videoTrack!=null)videoTrack.setEnabled(false);cameraEnabled=false;cameraState.accept(false);localFrame.accept(null);return;}
            if(cameras.isEmpty())cameras.addAll(MediaDevices.getVideoCaptureDevices());
            if(cameras.isEmpty())throw new IllegalStateException("Không tìm thấy camera.");
            if(selectedDevice<0||selectedDevice>=cameras.size())selectedDevice=0;
            if(videoSource!=null)videoSource.stop();
            if(videoSource==null){videoSource=new VideoDeviceSource();videoTrack=factory.createVideoTrack("darts-video",videoSource);videoTrack.addSink(localSink);}
            videoSourceDeviceIndex=selectedDevice;videoSource.setVideoCaptureDevice(cameras.get(selectedDevice));
            List<VideoCaptureCapability> caps=MediaDevices.getVideoCaptureCapabilities(cameras.get(selectedDevice));
            if(!caps.isEmpty())videoSource.setVideoCaptureCapability(caps.stream().min(Comparator.comparingInt(c->Math.abs(c.width-640)+Math.abs(c.height-360)+Math.max(0,c.frameRate-15)*8)).orElse(caps.get(0)));
            videoTrack.setEnabled(true);videoSender.replaceTrack(videoTrack);setVideoLimits();videoSource.start();
            cameraEnabled=true;cameraState.accept(true);
        }catch(RuntimeException e){if(videoSource!=null)try{videoSource.stop();}catch(RuntimeException ignored){}if(videoTrack!=null)videoTrack.setEnabled(false);cameraEnabled=false;cameraState.accept(false);localFrame.accept(null);error.accept("Không mở được camera: "+safeMessage(e));}
    });}
    public void selectCamera(int index){if(cameraEnabled)setCamera(true,index);}
    private int videoSourceDeviceIndex=-1;
    private void setVideoLimits(){try{RTCRtpSendParameters parameters=videoSender.getParameters();if(parameters.encodings!=null)for(RTCRtpEncodingParameters encoding:parameters.encodings){encoding.maxFramerate=15.0;encoding.maxBitrate=700_000;}videoSender.setParameters(parameters);}catch(RuntimeException ignored){}}
    private void attachRemoteVideo(VideoTrack track){
        if(remoteVideoTrack!=null)remoteVideoTrack.removeSink(remoteSink);
        remoteVideoTrack=track;track.addSink(remoteSink);
    }
    private static void renderFrame(VideoFrame frame,Consumer<Image> consumer,AtomicBoolean queued){
        if(!queued.compareAndSet(false,true))return;
        I420Buffer yuv=null;
        try {
            yuv=frame.buffer.toI420();int width=yuv.getWidth(),height=yuv.getHeight();
            if(width<=0||height<=0||width>1280||height>720){queued.set(false);return;}
            int[] pixels=new int[width*height];ByteBuffer y=yuv.getDataY(),u=yuv.getDataU(),v=yuv.getDataV();int sy=yuv.getStrideY(),su=yuv.getStrideU(),sv=yuv.getStrideV();
            for(int row=0;row<height;row++)for(int col=0;col<width;col++){
                int yy=(y.get(row*sy+col)&255)-16,uu=(u.get((row/2)*su+col/2)&255)-128,vv=(v.get((row/2)*sv+col/2)&255)-128;yy=Math.max(0,yy);
                int r=clamp((298*yy+409*vv+128)>>8),g=clamp((298*yy-100*uu-208*vv+128)>>8),b=clamp((298*yy+516*uu+128)>>8);
                pixels[row*width+col]=0xff000000|(r<<16)|(g<<8)|b;
            }
            int frameWidth=width,frameHeight=height;
            Platform.runLater(()->{try{WritableImage image=new WritableImage(frameWidth,frameHeight);image.getPixelWriter().setPixels(0,0,frameWidth,frameHeight,PixelFormat.getIntArgbInstance(),pixels,0,frameWidth);consumer.accept(image);}catch(RuntimeException ignored){}finally{queued.set(false);}});
        } catch(Throwable ignored){queued.set(false);}
        finally{if(yuv!=null)try{yuv.release();}catch(RuntimeException ignored){}}
    }
    private static int clamp(int x){return Math.max(0,Math.min(255,x));}
    private boolean ready(){return joined&&peerJoined&&peer!=null;}
    private void finishRemote(String message){closeResources();joined=false;peerJoined=false;callId=null;status.accept(message);micState.accept(false);cameraState.accept(false);}
    private void post(Runnable action){if(closed)return;try{worker.execute(()->{if(!closed)try{action.run();}catch(RuntimeException e){error.accept("Lỗi cuộc gọi: "+safeMessage(e));}});}catch(RejectedExecutionException ignored){}}
    private static String safeMessage(Throwable e){String text=e.getMessage();return text==null||text.isBlank()?e.getClass().getSimpleName():text;}
    private void closeResources(){
        if(audioSender!=null)try{audioSender.replaceTrack(null);}catch(RuntimeException ignored){}
        if(videoSender!=null)try{videoSender.replaceTrack(null);}catch(RuntimeException ignored){}
        if(videoSource!=null)try{videoSource.stop();}catch(RuntimeException ignored){}
        if(videoTrack!=null)try{videoTrack.removeSink(localSink);}catch(RuntimeException ignored){}
        if(peer!=null){try{peer.setAudioRecording(false);peer.setAudioPlayout(false);peer.close();}catch(RuntimeException ignored){}peer=null;}
        if(remoteVideoTrack!=null)try{remoteVideoTrack.removeSink(remoteSink);}catch(RuntimeException ignored){}
        if(audioSender!=null){try{audioSender.dispose();}catch(RuntimeException ignored){}audioSender=null;}
        if(videoSender!=null){try{videoSender.dispose();}catch(RuntimeException ignored){}videoSender=null;}
        if(audioTransceiver!=null){try{audioTransceiver.dispose();}catch(RuntimeException ignored){}audioTransceiver=null;}
        if(videoTransceiver!=null){try{videoTransceiver.dispose();}catch(RuntimeException ignored){}videoTransceiver=null;}
        if(remoteAudioTransceiver!=null){try{remoteAudioTransceiver.dispose();}catch(RuntimeException ignored){}remoteAudioTransceiver=null;}
        if(remoteVideoTransceiver!=null){try{remoteVideoTransceiver.dispose();}catch(RuntimeException ignored){}remoteVideoTransceiver=null;}
        if(remoteAudioReceiver!=null){try{remoteAudioReceiver.dispose();}catch(RuntimeException ignored){}remoteAudioReceiver=null;}
        if(remoteVideoReceiver!=null){try{remoteVideoReceiver.dispose();}catch(RuntimeException ignored){}remoteVideoReceiver=null;}
        if(remoteAudioTrack!=null){try{remoteAudioTrack.dispose();}catch(RuntimeException ignored){}remoteAudioTrack=null;}
        if(remoteVideoTrack!=null){try{remoteVideoTrack.dispose();}catch(RuntimeException ignored){}remoteVideoTrack=null;}
        if(audioTrack!=null){audioTrack.dispose();audioTrack=null;}
        if(audioSource!=null){audioSource.dispose();audioSource=null;}
        if(videoTrack!=null){try{videoTrack.dispose();}catch(RuntimeException ignored){}videoTrack=null;}
        if(videoSource!=null){try{videoSource.dispose();}catch(RuntimeException ignored){}videoSource=null;}
        if(factory!=null){factory.dispose();factory=null;}
        if(audioModule!=null){audioModule.dispose();audioModule=null;}
        pendingCandidates.clear();remoteDescriptionSet=false;microphoneEnabled=false;cameraEnabled=false;audioInputDeviceIndex=-1;videoSourceDeviceIndex=-1;devicesEnumerated=false;iceConfig=null;microphones.clear();speakers.clear();cameras.clear();
    }
    public void leave(){post(()->{if(joined){if(callId!=null)lastEndedCallId=callId;send.accept(Message.of(MessageType.MEDIA_LEAVE,matchId));}closeResources();joined=false;peerJoined=false;callId=null;status.accept("Đã rời cuộc gọi");micState.accept(false);cameraState.accept(false);localFrame.accept(null);remoteFrame.accept(null);});}
    public void cancel(){post(()->{closeResources();joined=false;peerJoined=false;callId=null;micState.accept(false);cameraState.accept(false);});}
    @Override public void close(){close(true);}
    public void close(boolean notifyServer){if(closed)return;closed=true;if(notifyServer&&joined)send.accept(Message.of(MessageType.MEDIA_LEAVE,matchId));worker.execute(this::closeResources);worker.shutdown();}
}
