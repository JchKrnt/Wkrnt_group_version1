package com.sohu.kurento.group;

import android.content.Context;
import android.opengl.EGLContext;
import android.util.Log;

import com.sohu.kurento.util.LogCat;
import com.sohu.kurento.util.LooperExecutor;

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
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jingbiaowang on 2015/12/4.
 */
public class KPeerConnectionClient {

    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String TAG = "PC_client";
    private static final String FIELD_TRIAL_VP9 = "WebRTC-SupportVP9/Enabled/";
    public static final String VIDEO_CODEC_VP8 = "VP8";
    public static final String VIDEO_CODEC_VP9 = "VP9";
    public static final String VIDEO_CODEC_H264 = "H264";
    public static final String AUDIO_CODEC_OPUS = "opus";
    public static final String AUDIO_CODEC_ISAC = "ISAC";
    private static final String VIDEO_CODEC_PARAM_START_BITRATE =
            "x-google-start-bitrate";
    private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
    private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
    private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
    private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
    private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
    private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
    private static final int HD_VIDEO_WIDTH = 1280;
    private static final int HD_VIDEO_HEIGHT = 720;
    private static final int MAX_VIDEO_WIDTH = 1280;
    private static final int MAX_VIDEO_HEIGHT = 1280;
    private static final int MAX_VIDEO_FPS = 30;

    //    private static final KPeerConnectionClient instance = new KPeerConnectionClient();
    private final PCObserver pcObserver = new PCObserver();
    private final SDPObserver sdpObserver = new SDPObserver();
    private final LooperExecutor executor;

    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    PeerConnectionFactory.Options options = null;
    private VideoSource videoSource;
    private boolean videoCallEnabled;
    private boolean preferIsac;
    private boolean preferH264;
    private boolean videoSourceStopped;
    private boolean isError;
    private Timer statsTimer;
    private ConnectionType connectionType;
    private MediaConstraints pcConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints audioConstraints;
    private MediaConstraints sdpMediaConstraints;
    private PeerConnectionParameters peerConnectionParameters;
    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    private LinkedList<IceCandidate> queuedRemoteCandidates;
    private KPeerConnectionEvents events;
    private boolean isInitiator;
    private SessionDescription localSdp; // either offer or answer SDP
    private MediaStream mediaStream;
    private int numberOfCameras;
    private VideoCapturerAndroid videoCapturer;
    // enableVideo is set to true if video should be rendered and sent.
    private boolean renderVideo;
    private VideoTrack localVideoTrack;
    private VideoTrack remoteVideoTrack;
    private List<PeerConnection.IceServer> iceServers;

    private ConcurrentHashMap<VideoRenderer.Callbacks, VideoRenderer> localRenders = new ConcurrentHashMap();

    private ConcurrentHashMap<VideoRenderer.Callbacks, VideoRenderer> remoteRenders = new ConcurrentHashMap();

    public KPeerConnectionClient() {

        LogCat.debug(TAG + " init peerconnection!");
        executor = new LooperExecutor();
        // Looper thread is started once in private ctor and is used for all
        // peer connection API calls to ensure new peer connection factory is
        // created on the same thread as previously destroyed factory.
        executor.requestStart();

    }

//    public static KPeerConnectionClient getInstance(){
//        return instance;
//    }

    public void setPeerConnectionFactoryOptions(PeerConnectionFactory.Options options) {
        this.options = options;
    }

    public static enum ConnectionType {
        READ_ONLY, SEND_ONLY, BOTH_WAY
    }

    /**
     * Peer connection parameters.
     */
    public static class PeerConnectionParameters {
        public final boolean videoCallEnabled;
        public final boolean loopback;
        public final int videoWidth;
        public final int videoHeight;
        public final int videoFps;
        public final int videoStartBitrate;
        public final String videoCodec;
        public final boolean videoCodecHwAcceleration;
        public final int audioStartBitrate;
        public final String audioCodec;
        public final boolean noAudioProcessing;
        public final boolean cpuOveruseDetection;

        public PeerConnectionParameters(
                boolean videoCallEnabled, boolean loopback,
                int videoWidth, int videoHeight, int videoFps, int videoStartBitrate,
                String videoCodec, boolean videoCodecHwAcceleration,
                int audioStartBitrate, String audioCodec,
                boolean noAudioProcessing, boolean cpuOveruseDetection) {
            this.videoCallEnabled = videoCallEnabled;
            this.loopback = loopback;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoFps = videoFps;
            this.videoStartBitrate = videoStartBitrate;
            this.videoCodec = videoCodec;
            this.videoCodecHwAcceleration = videoCodecHwAcceleration;
            this.audioStartBitrate = audioStartBitrate;
            this.audioCodec = audioCodec;
            this.noAudioProcessing = noAudioProcessing;
            this.cpuOveruseDetection = cpuOveruseDetection;
        }
    }


    public void createPeerConnectionFactory(
            final Context context,
            final PeerConnectionParameters peerConnectionParameters,
            final KPeerConnectionEvents events) {
        this.peerConnectionParameters = peerConnectionParameters;
        this.events = events;
        videoCallEnabled = peerConnectionParameters.videoCallEnabled;
        // Reset variables to initial states.
        factory = null;
        peerConnection = null;
        preferIsac = false;
        preferH264 = false;
        videoSourceStopped = false;
        isError = false;
        queuedRemoteCandidates = null;
        localSdp = null; // either offer or answer SDP
        mediaStream = null;
        videoCapturer = null;
        renderVideo = true;
        localVideoTrack = null;
        remoteVideoTrack = null;
        statsTimer = new Timer();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                createPeerConnectionFactoryInternal(context);
            }
        });
    }


    public void createPeerConnection(final EGLContext renderEGLContext, final ConnectionType connectionType,
                                     final List<PeerConnection.IceServer> iceServers) {

        LogCat.debug(TAG + "----createPeerConnection");
        this.connectionType = connectionType;
        this.iceServers = iceServers;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                createMediaConstraintsInternal();
                createPeerConnectionInternal(renderEGLContext);
            }
        });
    }

    private void createPeerConnectionFactoryInternal(Context context) {
        LogCat.debug(TAG + " Create peer connection factory. Use video: " +
                peerConnectionParameters.videoCallEnabled);
        isError = false;
        // Check if VP9 is used by default.
        if (videoCallEnabled && peerConnectionParameters.videoCodec != null
                && peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_VP9)) {
            PeerConnectionFactory.initializeFieldTrials(FIELD_TRIAL_VP9);
        } else {
            PeerConnectionFactory.initializeFieldTrials(null);
        }
        // Check if H.264 is used by default.
        preferH264 = false;
        if (videoCallEnabled && peerConnectionParameters.videoCodec != null
                && peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_H264)) {
            preferH264 = true;
        }
        // Check if ISAC is used by default.
        preferIsac = false;
        if (peerConnectionParameters.audioCodec != null
                && peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC)) {
            preferIsac = true;
        }
        if (!PeerConnectionFactory.initializeAndroidGlobals(context, true, true,
                peerConnectionParameters.videoCodecHwAcceleration)) {
            events.onPeerConnectionError("Failed to initializeAndroidGlobals");
        }
        factory = new PeerConnectionFactory();
        if (options != null) {
            LogCat.debug("Factory networkIgnoreMask option: " + options.networkIgnoreMask);
            factory.setOptions(options);
        }
        LogCat.debug("Peer connection factory created.");
    }

    private void createMediaConstraintsInternal() {

        LogCat.debug(TAG + " createMediaConstraintsInternal");
        // Create peer connection constraints.
        pcConstraints = new MediaConstraints();
        // Enable DTLS for normal calls and disable for loopback calls.
//        if (peerConnectionParameters.loopback) {
//            pcConstraints.optional.add(
//                    new KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "false"));
//        } else {
        pcConstraints.optional.add(
                new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
//        }

        // Create audio constraints.
        audioConstraints = new MediaConstraints();

        // added for audio performance measurements
        if (peerConnectionParameters.noAudioProcessing) {
            LogCat.debug("Disabling audio processing");
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        }

        if (connectionType != ConnectionType.READ_ONLY) {
            createLocalMediaConstraintsInternal();

        }


        // Create SDP constraints.
        sdpMediaConstraints = new MediaConstraints();
        if (connectionType == ConnectionType.SEND_ONLY) {
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveAudio", "false"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo", "false"));
        } else {
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveAudio", "true"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo", "true"));
        }

        LogCat.debug("-------created media constraint.");

    }

    private void createPeerConnectionInternal(EGLContext renderEGLContext) {

        LogCat.debug(TAG + " createPeerConnectionInternal");
        if (factory == null || isError) {
            Log.e(TAG, "Peerconnection factory is not created");
            return;
        }
        LogCat.debug("Create peer connection.");

        LogCat.debug("PCConstraints: " + pcConstraints.toString());
        if (videoConstraints != null) {
            LogCat.debug("VideoConstraints: " + videoConstraints.toString());
        }
        queuedRemoteCandidates = new LinkedList<IceCandidate>();

        if (videoCallEnabled) {
            LogCat.debug("EGLContext: " + renderEGLContext);
            factory.setVideoHwAccelerationOptions(renderEGLContext);
        }

        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        peerConnection = factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
        isInitiator = false;

        // Set default WebRTC tracing and INFO libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        Logging.enableTracing(
                "logcat:",
                EnumSet.of(Logging.TraceLevel.TRACE_INFO),
                Logging.Severity.LS_ERROR);
        if (connectionType != ConnectionType.READ_ONLY)
            creatLocalMediaStream();

        LogCat.debug("Peer connection created.");

    }

    /**
     * 创建local stream
     */
    private void creatLocalMediaStream() {

        LogCat.debug(TAG + " creatLocalMediaStream");
        mediaStream = factory.createLocalMediaStream("ARDAMS");
        if (videoCallEnabled) {
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
            mediaStream.addTrack(createVideoTrack(videoCapturer));
        }

        mediaStream.addTrack(factory.createAudioTrack(
                AUDIO_TRACK_ID,
                factory.createAudioSource(audioConstraints)));
        peerConnection.addStream(mediaStream);

    }

    private VideoTrack createVideoTrack(VideoCapturerAndroid capturer) {
        videoSource = factory.createVideoSource(capturer, videoConstraints);

        localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);

        localVideoTrack.setEnabled(renderVideo);
        //添加多个渲染view
        renderVideoTrack(localVideoTrack, localRenders);

        return localVideoTrack;
    }


    public void addLocalRenderer(final VideoRenderer.Callbacks callBack) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " addLocalRenderer");
                VideoRenderer videoRenderer = new VideoRenderer(callBack);
                if (localVideoTrack != null && connectionType != ConnectionType.READ_ONLY)
                    LogCat.debug(TAG + " addLocalRenderer now");
                localVideoTrack.addRenderer(videoRenderer);

                localRenders.put(callBack, videoRenderer);
            }
        });
    }

    public void addRemoteRender(final VideoRenderer.Callbacks callbacks) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " addRemoteRender");
                VideoRenderer videoRenderer = new VideoRenderer(callbacks);
                if (remoteVideoTrack != null && connectionType != ConnectionType.SEND_ONLY)
                    remoteVideoTrack.addRenderer(videoRenderer);

                remoteRenders.put(callbacks, videoRenderer);
            }
        });
    }

    public void removeLocalRenderer(VideoRenderer.Callbacks callbacks) {
        LogCat.debug(TAG + " removeLocalRenderer");
        removeRenderer(callbacks, localVideoTrack);
    }

    public void removeRemoteRenderer(VideoRenderer.Callbacks callbacks) {
        LogCat.debug(TAG + " removeRemoteRenderer");
        removeRenderer(callbacks, remoteVideoTrack);
    }


    private void removeRenderer(final VideoRenderer.Callbacks callbacks, final VideoTrack videoTrack) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " removeRenderer");

                VideoRenderer videoRenderer = localRenders.remove(callbacks);
                if (videoRenderer != null && videoTrack != null) {
                    videoTrack.removeRenderer(videoRenderer);
                }
            }
        });
    }

    private void renderVideoTrack(VideoTrack videoTrack, ConcurrentHashMap<VideoRenderer.Callbacks, VideoRenderer> renderers) {
        LogCat.debug(TAG + " renderVideoTrack");
        for (VideoRenderer videoRenderer : renderers.values()) {
            LogCat.debug(TAG + " renderVideoTrack size " + renderers.size());
            videoTrack.addRenderer(videoRenderer);
        }
    }

    private void createLocalMediaConstraintsInternal() {
        LogCat.debug(TAG + " createLocalMediaConstraintsInternal");
        // Check if there is a camera on device and disable video call if not.
        numberOfCameras = CameraEnumerationAndroid.getDeviceCount();
        if (numberOfCameras == 0) {
            Log.w(TAG, "No camera on device. Switch to audio only call.");
            videoCallEnabled = false;
        }
        // Create video constraints if video call is enabled.
        if (videoCallEnabled) {
            videoConstraints = new MediaConstraints();
            int videoWidth = peerConnectionParameters.videoWidth;
            int videoHeight = peerConnectionParameters.videoHeight;

            // If VP8 HW video encoder is supported and video resolution is not
            // specified force it to HD.
            if ((videoWidth == 0 || videoHeight == 0)
                    && peerConnectionParameters.videoCodecHwAcceleration
                    && MediaCodecVideoEncoder.isVp8HwSupported()) {
                videoWidth = HD_VIDEO_WIDTH;
                videoHeight = HD_VIDEO_HEIGHT;
            }

            // Add video resolution constraints.
            if (videoWidth > 0 && videoHeight > 0) {
                videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
                videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MIN_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MAX_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MIN_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MAX_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
            }

            // Add fps constraints.
            int videoFps = peerConnectionParameters.videoFps;
            if (videoFps > 0) {
                videoFps = Math.min(videoFps, MAX_VIDEO_FPS);
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                        MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
            }
        }

    }

    public static interface KPeerConnectionEvents {

        /**
         * Callback fired once local SDP is created and set.
         */
        public void onLocalDescription(final SessionDescription sdp);

        /**
         * Callback fired once local Ice candidate is generated.
         */
        public void onLocalIceCandidate(final IceCandidate candidate);

        /**
         * Callback fired once connection is established (IceConnectionState is
         * CONNECTED).
         */
        public void onIceConnected();

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * DISCONNECTED).
         */
        public void onIceDisconnected();

        /**
         * Callback fired once peer connection is closed.
         */
        public void onPeerConnectionClosed();

        /**
         * Callback fired once peer connection statistics is ready.
         */
        public void onPeerConnectionStatsReady(final StatsReport[] reports);

        /**
         * Callback fired once peer connection error happened.
         */
        public void onPeerConnectionError(final String description);
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
            Log.w(TAG, "No rtpmap for " + codec + " codec");
            return sdpDescription;
        }
        LogCat.debug("Found " + codec + " rtpmap " + codecRtpMap
                + " at " + lines[rtpmapLineIndex]);

        // Check if a=fmtp string already exist in remote SDP for this codec and
        // update it with new bitrate parameter.
        regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
        codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                LogCat.debug("Found " + codec + " " + lines[i]);
                if (isVideoCodec) {
                    lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE
                            + "=" + bitrateKbps;
                } else {
                    lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE
                            + "=" + (bitrateKbps * 1000);
                }
                LogCat.debug("Update remote SDP line: " + lines[i]);
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
                LogCat.debug("Add remote SDP line: " + bitrateSet);
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
            Log.w(TAG, "No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec);
            return sdpDescription;
        }
        LogCat.debug("Found " + codec + " rtpmap " + codecRtpMap + ", prefer at "
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
            LogCat.debug("Change media description: " + lines[mLineIndex]);
        } else {
            Log.e(TAG, "Wrong SDP media description format: " + lines[mLineIndex]);
        }
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

    private void reportError(final String errorMessage) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Peerconnection error: " + errorMessage);
                if (!isError) {
                    events.onPeerConnectionError(errorMessage);
                    isError = true;
                }
            }
        });
    }

    public void addRemoteIceCandidate(final IceCandidate candidate) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " addRemoteIceCandidate");
                if (peerConnection != null && !isError) {
                    if (queuedRemoteCandidates != null) {
                        queuedRemoteCandidates.add(candidate);
                    } else {
                        peerConnection.addIceCandidate(candidate);
                    }
                }
            }
        });
    }

    public void setRemoteDescription(final SessionDescription sdp) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " setRemoteDescription");
                if (peerConnection == null || isError) {
                    return;
                }
                String sdpDescription = sdp.description;
                if (preferIsac) {
                    sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
                }
                if (videoCallEnabled && preferH264) {
                    sdpDescription = preferCodec(sdpDescription, VIDEO_CODEC_H264, false);
                }
                if (videoCallEnabled && peerConnectionParameters.videoStartBitrate > 0) {
                    sdpDescription = setStartBitrate(VIDEO_CODEC_VP8, true,
                            sdpDescription, peerConnectionParameters.videoStartBitrate);
                    sdpDescription = setStartBitrate(VIDEO_CODEC_VP9, true,
                            sdpDescription, peerConnectionParameters.videoStartBitrate);
                    sdpDescription = setStartBitrate(VIDEO_CODEC_H264, true,
                            sdpDescription, peerConnectionParameters.videoStartBitrate);
                }
                if (peerConnectionParameters.audioStartBitrate > 0) {
                    sdpDescription = setStartBitrate(AUDIO_CODEC_OPUS, false,
                            sdpDescription, peerConnectionParameters.audioStartBitrate);
                }
                LogCat.debug("Set remote SDP.");
                SessionDescription sdpRemote = new SessionDescription(
                        sdp.type, sdpDescription);
                peerConnection.setRemoteDescription(sdpObserver, sdpRemote);
            }
        });
    }

    public void setVideoEnabled(final boolean enable) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " setVideoEnabled");
                renderVideo = enable;
                if (localVideoTrack != null) {
                    localVideoTrack.setEnabled(renderVideo);
                }
                if (remoteVideoTrack != null) {
                    remoteVideoTrack.setEnabled(renderVideo);
                }
            }
        });
    }

    public void createOffer() {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " createOffer");
                if (peerConnection != null && !isError) {
                    LogCat.debug("PC Create OFFER");
                    isInitiator = true;
                    peerConnection.createOffer(sdpObserver, sdpMediaConstraints);
                }
            }
        });
    }

    public void stopVideoSource() {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " stopVideoSource");
                if (videoSource != null && !videoSourceStopped) {
                    if (peerConnection != null) {
                        LogCat.debug("Stop video source.");
                        videoSource.stop();
                        videoSourceStopped = true;
                    }
                }
            }
        });
    }

    public void startVideoSource() {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                LogCat.debug(TAG + " startVideoSource");
                if (videoSource != null && videoSourceStopped) {
                    if (peerConnection != null) {
                        LogCat.debug("Restart video source.");
                        videoSource.restart();
                        videoSourceStopped = false;
                    }
                }
            }
        });
    }


    private void switchCameraInternal() {
        if (!videoCallEnabled || numberOfCameras < 2 || isError || videoCapturer == null) {
            Log.e(TAG, "Failed to switch camera. Video: " + videoCallEnabled + ". Error : "
                    + isError + ". Number of cameras: " + numberOfCameras);
            return;  // No video is sent or only one camera is available or error happened.
        }
        LogCat.debug("Switch camera");
        videoCapturer.switchCamera(null);
    }

    public void switchCamera() {
        LogCat.debug(TAG + " switchCamera");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                switchCameraInternal();
            }
        });
    }

    // Implementation detail: handle offer creation/signaling and answer setting,
    // as well as adding remote ICE candidates once the answer SDP is set.
    private class SDPObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            if (localSdp != null) {
                reportError("Multiple SDP create.");
                return;
            }
            String sdpDescription = origSdp.description;
            if (preferIsac) {
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
            }
            if (videoCallEnabled && preferH264) {
                sdpDescription = preferCodec(sdpDescription, VIDEO_CODEC_H264, false);
            }
            final SessionDescription sdp = new SessionDescription(
                    origSdp.type, sdpDescription);
            localSdp = sdp;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection != null && !isError) {
                        LogCat.debug("Set local SDP from " + sdp.type);
                        peerConnection.setLocalDescription(sdpObserver, sdp);
                    }
                }
            });
        }

        @Override
        public void onSetSuccess() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection == null || isError) {
                        return;
                    }
                    if (isInitiator) {
                        // For offering peer connection we first create offer and set
                        // local SDP, then after receiving answer set remote SDP.
                        if (peerConnection.getRemoteDescription() == null) {
                            // We've just set our local SDP so time to send it.
                            LogCat.debug("Local SDP set succesfully");
                            events.onLocalDescription(localSdp);
                        } else {
                            // We've just set remote description, so drain remote
                            // and send local ICE candidates.
                            LogCat.debug("Remote SDP set succesfully");
                            drainCandidates();
                        }
                    } else {
                        // For answering peer connection we set remote SDP and then
                        // create answer and set local SDP.
                        if (peerConnection.getLocalDescription() != null) {
                            // We've just set our local SDP so time to send it, drain
                            // remote and send local ICE candidates.
                            LogCat.debug("Local SDP set succesfully");
                            events.onLocalDescription(localSdp);
                            drainCandidates();
                        } else {
                            // We've just set remote SDP - do nothing for now -
                            // answer will be created soon.
                            LogCat.debug("Remote SDP set succesfully");
                        }
                    }
                }
            });
        }

        @Override
        public void onCreateFailure(final String error) {
            reportError("createSDP error: " + error);
        }

        @Override
        public void onSetFailure(final String error) {
            reportError("setSDP error: " + error);
        }
    }

    private void drainCandidates() {
        if (queuedRemoteCandidates != null) {
            LogCat.debug("Add " + queuedRemoteCandidates.size() + " remote candidates");
            for (IceCandidate candidate : queuedRemoteCandidates) {
                peerConnection.addIceCandidate(candidate);
            }
            queuedRemoteCandidates = null;
        }
    }

    // Implementation detail: observe ICE & stream changes and react accordingly.
    private class PCObserver implements PeerConnection.Observer {
        String pcObserverLogMsg = "PC observer ";

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            executor.execute(new Runnable() {
                @Override
                public void run() {

                    LogCat.debug("----local ice candidate---");
                    events.onLocalIceCandidate(candidate);
                }
            });
        }

        @Override
        public void onSignalingChange(
                PeerConnection.SignalingState newState) {
            LogCat.debug("SignalingState: " + newState);
        }

        @Override
        public void onIceConnectionChange(
                final PeerConnection.IceConnectionState newState) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    LogCat.v(pcObserverLogMsg + "onIceConnectionChange IceConnectionState : " +
                            newState.name());
                    if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                        events.onIceConnected();
                    } else if (newState == PeerConnection.IceConnectionState.CLOSED) {
                        events.onIceDisconnected();
                    } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                        reportError("ICE connection failed.");
                    }
                }
            });
        }

        @Override
        public void onIceGatheringChange(
                PeerConnection.IceGatheringState newState) {
            LogCat.v(pcObserverLogMsg + "onIceGatheringChange IceGatheringState : " + newState
                    .name());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            LogCat.v("IceConnectionReceiving changed to " + receiving);
        }

        @Override
        public void onAddStream(final MediaStream stream) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    LogCat.v("onAddStream -- video size " + stream.videoTracks.size() + " audio size " + stream.audioTracks.size());

                    if (peerConnection == null || isError) {
                        return;
                    }
                    if (stream.audioTracks.size() > 1 || stream.videoTracks.size() > 1) {
                        reportError("Weird-looking stream: " + stream);
                        return;
                    }
                    if (stream.videoTracks.size() >= 1) {
                        remoteVideoTrack = stream.videoTracks.get(0);

                        if (connectionType != ConnectionType.SEND_ONLY) {
                            remoteVideoTrack.setEnabled(renderVideo);
//                            remoteVideoTrack.addRenderer(new VideoRenderer(connectionType.getRemoteRender()));
                            //添加多个渲染view。
                            renderVideoTrack(remoteVideoTrack, remoteRenders);
                        }
                    }
                }
            });
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    LogCat.v(pcObserverLogMsg + " onRemoveStream MediaStream : " + stream.label());
                    remoteVideoTrack = null;
                    if (stream.videoTracks.size() >= 1) {
                        stream.videoTracks.get(0).dispose();
                        stream.audioTracks.get(0).dispose();
                    }
                }
            });
        }

        @Override
        public void onDataChannel(final DataChannel dc) {

            LogCat.e(pcObserverLogMsg + "onDataChannel DataChannel : " + dc.label());
            reportError("AppRTC doesn't use data channels, but got: " + dc.label()
                    + " anyway!");
        }

        @Override
        public void onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
            LogCat.v(pcObserverLogMsg + "onRenegotiationNeeded .");
        }
    }

    public void close() {

        executor.execute(new Runnable() {
            @Override
            public void run() {

                LogCat.debug(TAG + " close()");
                closeInternal();
            }
        });
    }


    private void closeInternal() {

        LogCat.debug("Closing peer connection.");
        statsTimer.cancel();
        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }

        LogCat.debug("Closing video Track.");
        if (localVideoTrack != null) {
            LogCat.debug("localVideo Track " + localVideoTrack.state());
            for (VideoRenderer render :
                    localRenders.values()) {
                localVideoTrack.removeRenderer(render);
            }

//            localVideoTrack.dispose();

            localVideoTrack = null;
        }

        LogCat.debug("before video dispose...");
        if (videoSource != null) {
            LogCat.debug("video source state " + videoSource.state());
            videoSource.dispose();
            LogCat.debug("video source state " + videoSource.state());
            videoSource = null;
        }

        LogCat.debug("Closing peer connection factory.");
        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        options = null;

        LogCat.debug("Closing peer connection done.");
        events.onPeerConnectionClosed();
    }
}
