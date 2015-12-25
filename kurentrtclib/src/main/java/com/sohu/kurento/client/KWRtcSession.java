package com.sohu.kurento.client;

import android.content.Context;
import android.opengl.EGLContext;

import com.sohu.kurento.bean.SettingsBean;
import com.sohu.kurento.bean.UserType;
import com.sohu.kurento.netClient.KWEvent;
import com.sohu.kurento.util.LogCat;
import com.sohu.kurento.util.LooperExecutor;

import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jingbiaowang on 2015/7/24.
 */
public class KWRtcSession implements KWSessionEvent {

    private LooperExecutor executor;
    private static KWRtcSession instance;
    private PeerConnectionFactory.Options options;
    private SettingsBean sessionParams;
    private KWEvent evnent;
    private PeerConnectionFactory factory;

    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String FIELD_TRIAL_VP9 = "WebRTC-SupportVP9/Enabled/";
    private static final String VIDEO_CODEC_VP8 = "VP8";
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String VIDEO_CODEC_H264 = "H264";
    private static final String AUDIO_CODEC_OPUS = "opus";
    private static final String AUDIO_CODEC_ISAC = "ISAC";
    private static final String VIDEO_CODEC_PARAM_START_BITRATE =
            "x-google-start-bitrate";
    private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
    private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
    private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
    private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
    private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
    private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
    private static final String RTPDATACHANNELS = "RtpDataChannels";
    private static final int HD_VIDEO_WIDTH = 480;
    private static final int HD_VIDEO_HEIGHT = 640;
    private static final int MAX_VIDEO_WIDTH = 480;
    private static final int MAX_VIDEO_HEIGHT = 640;
    private static final int MAX_VIDEO_FPS = 15;
    //保持現成同步。
    private boolean isError;
    private boolean preferH264;
    private boolean preferIsac;
    private MediaConstraints pcConstraints;
    private int numberOfCameras;
    private MediaConstraints localMediaConstraints;
    private MediaConstraints localAudioConstraints;
    private MediaConstraints sdpMediaConstraints;
    private PeerConnection peerConnection;
    private final KWPeerConnectionObserver pcObserver = new KWPeerConnectionObserver();
    private MediaStream mediaStream;
    private VideoCapturerAndroid videoCapturer;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private VideoTrack remoteVideoTrack;
    //控制是否显示localVideo, remoteVideo?
    private boolean localRenderVideo;
    private boolean remoteRenderVideo;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private final KWSdpObserver sdpObserver = new KWSdpObserver();
    private boolean videoSourceStopped;

    private KWRtcSession() {
        executor = new LooperExecutor();
        executor.requestStart();

    }

    public static KWRtcSession getInstance() {
        if (instance == null) {
            instance = new KWRtcSession();
        }

        return instance;
    }

    public void setPeerConnectionFactoryOptions(PeerConnectionFactory.Options options) {
        this.options = options;
    }

    public void createPeerConnectionFactory(final VideoRenderer.Callbacks localRender,
                                            final VideoRenderer.Callbacks remoteRender,
                                            SettingsBean sessionParams, final Context context,
                                            final EGLContext reanderEGLContext, KWEvent evnent) {
        this.sessionParams = sessionParams;
        numberOfCameras = 0;
        preferH264 = false;
        preferIsac = false;
        pcConstraints = null;
        localMediaConstraints = null;
        localAudioConstraints = null;
        sdpMediaConstraints = null;
        isError = false;
        peerConnection = null;
        mediaStream = null;
        videoCapturer = null;
        localVideoTrack = null;
        remoteVideoTrack = null;
        localRenderVideo = sessionParams.getUserType() == UserType.VIEWER ? false : true;
        remoteRenderVideo = !localRenderVideo;
        videoSourceStopped = false;
        this.remoteRender = remoteRender;
        this.localRender = localRender;
        this.evnent = evnent;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                createPeerConnectionFactoryInternal(context, reanderEGLContext);
            }
        });
    }

    public void stopVideoSource() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (videoSource != null && !videoSourceStopped) {
                    LogCat.debug("Stop video source.");
                    videoSource.stop();
                    videoSourceStopped = true;
                }
            }
        });
    }

    public void startVideoSource() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (videoSource != null && videoSourceStopped) {
                    LogCat.debug("Restart video source.");
                    videoSource.restart();
                    videoSourceStopped = false;
                }
            }
        });
    }

    /**
     * Init peerConnection global, create peerconnection factory.
     *
     * @param context
     * @param renderEGLContext
     */
    private void createPeerConnectionFactoryInternal(Context context, EGLContext
            renderEGLContext) {

        LogCat.debug("Create peer connection factory with EGLContext "
                + renderEGLContext + ". Use video: "
                + sessionParams.isVideoCallEnable());
//        isError = false;
        // Check if VP9 is used by default.
        if (sessionParams.isVideoCallEnable() && sessionParams.getVideoCode() != null
                && sessionParams.getVideoCode().equals(VIDEO_CODEC_VP9)) {
            PeerConnectionFactory.initializeFieldTrials(FIELD_TRIAL_VP9);
        } else {
            PeerConnectionFactory.initializeFieldTrials(null);
        }
        // Check if H.264 is used by default.
        preferH264 = false;
        if (sessionParams.isVideoCallEnable() && sessionParams.getVideoCode() != null
                && sessionParams.getVideoCode().equals(VIDEO_CODEC_H264)) {
            preferH264 = true;
        }
        // Check if ISAC is used by default.
        preferIsac = false;
        if (sessionParams.getAudioCode() != null
                && sessionParams.getAudioCode().equals(AUDIO_CODEC_ISAC)) {
            preferIsac = true;
        }

        //init PeerConnection global.
        if (!PeerConnectionFactory.initializeAndroidGlobals(
                context, true, true,
                sessionParams.isHwCodeEnable())) {
            evnent.portError("Failed to initializeAndroidGlobals");
        }
        factory = new PeerConnectionFactory();
        if (options != null) {
            LogCat.debug("Factory networkIgnoreMask option: " + options.networkIgnoreMask);
            factory.setOptions(options);
        }
        LogCat.debug("Peer connection factory created.");

    }

    /**
     * create mediaConstraints , 创建peerConnection.
     */
    public void createPeerConnection() {

        executor.execute(new Runnable() {
            @Override
            public void run() {

                createMediaConstraintsInternal();
                createPeerConnectionInternal();
            }
        });
    }


    private void createMediaConstraintsInternal() {
        LogCat.debug("-------create media constraint.");
        // Create peer connection constraints.
        pcConstraints = new MediaConstraints();
        // Enable DTLS for normal calls and disable for loopback calls.
        pcConstraints.optional.add(
                new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
        pcConstraints.optional.add(
                new MediaConstraints.KeyValuePair(RTPDATACHANNELS, "true"));
        //create local Constraints
        if (sessionParams.getUserType() != UserType.VIEWER)
            createLocalMediaConstraintsInernal();

        // Create SDP constraints.
        sdpMediaConstraints = new MediaConstraints();

        if (sessionParams.getUserType() == UserType.PRESENTER) {       //master.
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo", "false"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveAudio", "false"));
        } else {            //viewer.
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo", "true"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveAudio", "true"));
        }

        LogCat.debug("-------created media constraint.");
    }

    private void createLocalMediaConstraintsInernal() {
        // Check if there is a camera on device and disable video call if not.
        LogCat.debug("-------create local media constraint.");
        numberOfCameras = CameraEnumerationAndroid.getDeviceCount();
        if (numberOfCameras == 0) {
            LogCat.debug("No camera on device. Switch to audio only call.");
            sessionParams.setVideoCallEnable(false);
        }
        // Create video constraints if video call is enabled.
        if (sessionParams.isVideoCallEnable()) {
            localMediaConstraints = new MediaConstraints();
            int videoWidth = sessionParams.getVideoWidth();
            int videoHeight = sessionParams.getVideoHeight();

            // If VP8 HW video encoder is supported and video resolution is not
            // specified force it to HD.
            if ((videoWidth == 0 || videoHeight == 0)
                    && sessionParams.isHwCodeEnable()
                    && MediaCodecVideoEncoder.isVp8HwSupported()) {
                videoWidth = HD_VIDEO_WIDTH;
                videoHeight = HD_VIDEO_HEIGHT;
            }

            // Add video resolution constraints.
            if (videoWidth > 0 && videoHeight > 0) {
                videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
                videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
                localMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MIN_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
                localMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MAX_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
                localMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MIN_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
                localMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MAX_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
            }

            // Add fps constraints.
            int videoFps = sessionParams.getFps();
            if (videoFps > 0) {
                videoFps = Math.min(videoFps, MAX_VIDEO_FPS);
                localMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
                localMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
            }
        }

        // Create audio constraints.
        localAudioConstraints = new MediaConstraints();

        LogCat.debug("-------created local media constraint.");

    }

    private void createPeerConnectionInternal() {

        if (factory == null || isError) {
            LogCat.e("Peerconnection factory is not created");
            return;
        }
        LogCat.debug("Create peer connection");
        LogCat.debug("PCConstraints: " + pcConstraints.toString());
        if (localMediaConstraints != null) {
            LogCat.debug("VideoConstraints: " + localMediaConstraints.toString());
        }

        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(getIces());
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;

        peerConnection = factory.createPeerConnection(
                rtcConfig, pcConstraints, pcObserver);
        LogCat.debug(" peer connection created--------");
        // Set default WebRTC tracing and INFO libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        Logging.enableTracing(
                "logcat:",
                EnumSet.of(Logging.TraceLevel.TRACE_DEBUG),
                Logging.Severity.LS_INFO);
        createLocalMediaStream();
        LogCat.debug(" peer connection created--------");

    }

    /**
     * 创建 local mediaStream.
     */
    private void createLocalMediaStream() {
        LogCat.debug(" create local media Stream.--------");
        mediaStream = factory.createLocalMediaStream("ARDAMS");
        if (sessionParams.getUserType() == UserType.PRESENTER || sessionParams.getUserType() == UserType.BOTH) {
            if (sessionParams.isVideoCallEnable()) {
                String cameraDeviceName = CameraEnumerationAndroid.getDeviceName(0);
                String frontCameraDeviceName =
                        CameraEnumerationAndroid.getNameOfFrontFacingDevice();
                if (numberOfCameras > 1 && frontCameraDeviceName != null) {
                    cameraDeviceName = frontCameraDeviceName;
                }
                LogCat.debug("Opening camera: " + cameraDeviceName);
                videoCapturer = VideoCapturerAndroid.create(cameraDeviceName, null);
                if (videoCapturer == null) {
                    reportError("Failed to open camera");
                    return;
                }

                videoSource = factory.createVideoSource(videoCapturer, localMediaConstraints);

                localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);

                localVideoTrack.setEnabled(localRenderVideo);

                localVideoTrack.addRenderer(new VideoRenderer(localRender));

                mediaStream.addTrack(localVideoTrack);
            }
            AudioTrack audioTrack = factory.createAudioTrack(
                    AUDIO_TRACK_ID,
                    factory.createAudioSource(localAudioConstraints));
            audioTrack.setEnabled(localRenderVideo);

            mediaStream.addTrack(audioTrack);
        }

        peerConnection.addStream(mediaStream);
        LogCat.debug("local stream added");
    }

    /**
     * @param enable
     */
    public void setLocalVideoEnabled(final boolean enable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                localRenderVideo = enable;
                if (localVideoTrack != null) {
                    localVideoTrack.setEnabled(localRenderVideo);
                }

            }
        });
    }

    public void setRomoteVideoEnabled(final boolean enable) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                remoteRenderVideo = enable;
                if (remoteVideoTrack != null) {
                    remoteVideoTrack.setEnabled(remoteRenderVideo);
                }
            }
        });
    }

    @Override
    public void createOffer() {
        LogCat.debug("create offer-------");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnection != null)
                    peerConnection.createOffer(sdpObserver, sdpMediaConstraints);
                else reportError("peerConnection is null oncreateOffer.");
            }
        });
    }

    private void reportError(final String msg) {
        if (!isError)
            isError = true;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                evnent.portError(msg);

            }
        });
    }


    @Override
    public void setRemoteSdp(String remoteSdp) {
        if (peerConnection == null || isError) {
            return;
        }
        LogCat.v("remote SDP : before : " + remoteSdp);
        if (preferIsac) {
            remoteSdp = preferCodec(remoteSdp, AUDIO_CODEC_ISAC, true);
        }
        if (sessionParams.isVideoCallEnable() && preferH264) {
            remoteSdp = preferCodec(remoteSdp, VIDEO_CODEC_H264, false);
        }
        if (sessionParams.isVideoCallEnable() && sessionParams.getStartVideoBitrateValue() > 0) {
            remoteSdp = setStartBitrate(VIDEO_CODEC_VP8, true,
                    remoteSdp, sessionParams.getStartVideoBitrateValue());
            remoteSdp = setStartBitrate(VIDEO_CODEC_VP9, true,
                    remoteSdp, sessionParams.getStartVideoBitrateValue());
            remoteSdp = setStartBitrate(VIDEO_CODEC_H264, true,
                    remoteSdp, sessionParams.getStartVideoBitrateValue());
        }
        if (sessionParams.getAudioBitrateValue() > 0) {
            remoteSdp = setStartBitrate(AUDIO_CODEC_OPUS, false,
                    remoteSdp, sessionParams.getAudioBitrateValue());
        }
        LogCat.debug("Set remote SDP.");
        LogCat.v("remote SDP : " + remoteSdp);
        final SessionDescription sdpRemote = new SessionDescription(
                SessionDescription.Type.ANSWER, remoteSdp);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                peerConnection.setRemoteDescription(sdpObserver, sdpRemote);
            }
        });

    }

    @Override
    public void dispose() {

    }

    private class KWPeerConnectionObserver implements PeerConnection.Observer {

        String pcObserverLogMsg = "PC observer ";

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
            LogCat.v(pcObserverLogMsg + "onSignalingChange signalingState : " + newState.name());
        }

        @Override
        public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    LogCat.v(pcObserverLogMsg + "onIceConnectionChange IceConnectionState : " +
                            newState.name());
                    if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                        evnent.onIceConnected();
                    } else if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
                        evnent.onIceDisconnected();
                    } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                        reportError("ICE connection failed.");
                    }
                }
            });
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            LogCat.v("IceConnectionReceiving changed to " + receiving);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
            LogCat.v(pcObserverLogMsg + "onIceGatheringChange IceGatheringState : " + newState
                    .name());

        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            LogCat.v(pcObserverLogMsg + "onIceCandidate IceCandidate : " + candidate.sdp);
            if (peerConnection != null)
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        evnent.onIceCandidate(candidate);
                    }
                });
        }

        @Override
        public void onAddStream(final MediaStream stream) {
            LogCat.v(pcObserverLogMsg + "onAddStream MediaStream : " + stream.label());
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection == null || isError) {
                        return;
                    }
                    if (stream.audioTracks.size() > 1 || stream.videoTracks.size() > 1) {
                        reportError("Weird-looking stream: " + stream);
                        return;
                    }

//                    if (stream.audioTracks.size() == 1 && sessionParams.getUserType() ==
// UserType.PRESENTER) {
//                        AudioTrack audioTrack = stream.audioTracks.get(0);
//                        audioTrack.setEnabled(false);
//                    }
                    if (stream.videoTracks.size() == 1) {
                        remoteVideoTrack = stream.videoTracks.get(0);
                        remoteVideoTrack.setEnabled(sessionParams.getUserType() == UserType
                                .PRESENTER ? false : true);
                        remoteVideoTrack.addRenderer(new VideoRenderer(remoteRender));
                    }

                }
            });
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {
            LogCat.v(pcObserverLogMsg + "onRemoveStream MediaStream : " + stream.label());
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection == null || isError) {
                        return;
                    }
                    remoteVideoTrack = null;
                    stream.videoTracks.get(0).dispose();
                    stream.audioTracks.get(0).dispose();
                }
            });
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            LogCat.v(pcObserverLogMsg + "onDataChannel DataChannel : " + dataChannel.label());
            reportError("AppRTC doesn't use data channels, but got: " + dataChannel.label()
                    + " anyway!");
        }

        @Override
        public void onRenegotiationNeeded() {
            LogCat.v(pcObserverLogMsg + "onRenegotiationNeeded .");
        }
    }

//    private class KWSdpObserver implements

    private List<PeerConnection.IceServer> getIces() {
        String[] stunAddresses = new String[]{
//                "220.181.90.108:3478",
//                "61.135.176.88:3478",
//                "220.181.90.110:3478",
//                "220.181.90.108:3479",
//                "61.135.176.88:3479",
//                "220.181.90.108:3478"

        };

        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
//        StringBuilder stunAddress = new StringBuilder();
//        for (String address : stunAddresses) {
//            stunAddress.append("stun:").append(address);
//            PeerConnection.IceServer iceServer = new PeerConnection.IceServer(stunAddress
// .toString());
//            iceServers.add(iceServer);
//            stunAddress.delete(0, stunAddress.length());
//        }

        iceServers.add(new PeerConnection.IceServer("stun:47.88.1.81:3478"));
//        iceServers.add(new PeerConnection.IceServer("turn:47.88.1.81:3478", "u1", "u1"));

        return iceServers;
    }

    private class KWSdpObserver implements SdpObserver {


        @Override
        public void onCreateSuccess(SessionDescription origSdp) {
            LogCat.debug("local sdp created----------");
            if (peerConnection.getLocalDescription() != null) {
                reportError("Multiple SDP create.");
                return;
            }
            String sdpDescription = origSdp.description;
            if (preferIsac) {
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
            }
            if (preferH264) {
                sdpDescription = preferCodec(sdpDescription, VIDEO_CODEC_H264, false);
            }
            final SessionDescription sdp = new SessionDescription(
                    origSdp.type, sdpDescription);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection != null && !isError) {
                        LogCat.debug("Set local SDP from " + sdp.type);

                        LogCat.v("local sdp description ---:---: " + sdp.description);
                        peerConnection.setLocalDescription(sdpObserver, sdp);
                        LogCat.v("local sdp description ---:---: " + sdp.description);

                    }
                }
            });

        }

        @Override
        public void onSetSuccess() {

            if (peerConnection.getLocalDescription() != null && peerConnection
                    .getRemoteDescription() == null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        LogCat.debug("KW set local description successs !");
                        evnent.onLocalSdp(peerConnection.getLocalDescription());
                    }
                });
            } else if (peerConnection.getRemoteDescription() != null) {
                LogCat.debug("KW set remote description success !");
            }
        }

        @Override
        public void onCreateFailure(String error) {
            reportError("createSDP error: " + error);
        }

        @Override
        public void onSetFailure(String error) {
            reportError("setSDP error: " + error);
        }
    }


    private static String setStartBitrate(String codec, boolean isVideoCodec,
                                          String sdpDescription, int bitrateKbps) {
        String[] lines = sdpDescription.split("\r\n");
        int rtpmapLineIndex = -1;
        boolean sdpFormatUpdated = false;
        String codecRtpMap = null;
        // Search for codec rtpmap in format
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                rtpmapLineIndex = i;
                break;
            }
        }
        if (codecRtpMap == null) {
            LogCat.w("No rtpmap for " + codec + " codec");
            return sdpDescription;
        }
        LogCat.w("Found " + codec + " rtpmap " + codecRtpMap
                + " at " + lines[rtpmapLineIndex]);

        // Check if a=fmtp string already exist in remote SDP for this codec and
        // update it with new bitrate parameter.
        regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
        codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                LogCat.w("Found " + codec + " " + lines[i]);
                if (isVideoCodec) {
                    lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE
                            + "=" + bitrateKbps;
                } else {
                    lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE
                            + "=" + (bitrateKbps * 1000);
                }
                LogCat.w("Update remote SDP line: " + lines[i]);
                sdpFormatUpdated = true;
                break;
            }
        }

        StringBuilder newSdpDescription = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            newSdpDescription.append(lines[i]).append("\r\n");
            // Append new a=fmtp line if no such line exist for a codec.
            if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                String bitrateSet;
                if (isVideoCodec) {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " "
                            + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
                } else {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " "
                            + AUDIO_CODEC_PARAM_BITRATE + "=" + (bitrateKbps * 1000);
                }
                LogCat.w("Add remote SDP line: " + bitrateSet);
                newSdpDescription.append(bitrateSet).append("\r\n");
            }

        }
        return newSdpDescription.toString();
    }

    private static String preferCodec(
            String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        String mediaDescription = "m=video ";
        if (isAudio) {
            mediaDescription = "m=audio ";
        }
        for (int i = 0; (i < lines.length)
                && (mLineIndex == -1 || codecRtpMap == null); i++) {
            if (lines[i].startsWith(mediaDescription)) {
                mLineIndex = i;
                continue;
            }
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
            LogCat.w("No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        }
        if (codecRtpMap == null) {
            LogCat.w("No rtpmap for " + codec);
            return sdpDescription;
        }
        LogCat.w("Found " + codec + " rtpmap " + codecRtpMap + ", prefer at "
                + lines[mLineIndex]);
        String[] origMLineParts = lines[mLineIndex].split(" ");
        if (origMLineParts.length > 3) {
            StringBuilder newMLine = new StringBuilder();
            int origPartIndex = 0;
            // Format is: m=<media> <port> <proto> <fmt> ...
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(codecRtpMap);
            for (; origPartIndex < origMLineParts.length; origPartIndex++) {
                if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
                    newMLine.append(" ").append(origMLineParts[origPartIndex]);
                }
            }
            lines[mLineIndex] = newMLine.toString();
            LogCat.w("Change media description: " + lines[mLineIndex]);
        } else {
            LogCat.e("Wrong SDP media description format: " + lines[mLineIndex]);
        }
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }


    @Override
    public void addRemoteCandidate(final IceCandidate candidate) {

        LogCat.debug("add remote candidate : " + candidate.toString());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                peerConnection.addIceCandidate(candidate);

            }
        });
    }

    public void close() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                closeInternal();
            }
        });
    }

    private void closeInternal() {
//        statsTimer.cancel();
        if (peerConnection != null) {
            LogCat.debug("Closing peer connection.");
            peerConnection.dispose();
            peerConnection = null;
        }
        LogCat.debug("Closing video source.");
        if (videoSource != null) {
            LogCat.debug("Closing video source.");
            videoSource.dispose();
            videoSource = null;
        }

        LogCat.debug("Closing peer connection factory.");
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
        options = null;
        LogCat.debug("Closing peer connection done.");
        evnent.onPeerConnectionClosed();
    }
}
