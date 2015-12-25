package com.sohu.jch.krnt_android_group.controller;

import android.content.Context;

import com.sohu.jch.krnt_android_group.view.play.PercentFrameLayout;
import com.sohu.kurento.bean.VideoViewParam;
import com.sohu.kurento.group.KPeerConnectionClient;
import com.sohu.kurento.group.KPeerConnectionClient.ConnectionType;
import com.sohu.kurento.util.LogCat;
import com.sohu.kurento.util.SinglExecterPool;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;

/**
 * Created by jingbiaowang on 2015/12/4.
 */
public class Participant implements KPeerConnectionClient.KPeerConnectionEvents {

    private KPeerConnectionClient peer;
    private String name;
    private Context context;
    private EglBase eglBase;
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private PercentFrameLayout localContainerLayout;
    private PercentFrameLayout remoteContainerLayout;
    private VideoViewParam localVideoParam;
    private VideoViewParam remoteVideoParam;
    private ConnectionType connectionType;
    private boolean iceConnected = false;
    private ParticipantEvents participantEvents;

    public Participant(Context context, EglBase eglBase, String name, ConnectionType connectionType) {

        this.peer = new KPeerConnectionClient();
        this.name = name;
        this.context = context;
        this.connectionType = connectionType;
        this.eglBase = eglBase;
    }

    public void updateVideoView() {
        if (connectionType != ConnectionType.READ_ONLY) {
            updateLocalVideoView();
        }

        if (connectionType != ConnectionType.SEND_ONLY) {
            updateRemoteVideoView();
        }
    }

    private void updateLocalVideoView() {
        if (localVideoView != null) {
            if (iceConnected) {
                localContainerLayout.setPosition(       //connected view.
                        localVideoParam.getLayoutPositionX(), localVideoParam.getLayoutPositionY(), localVideoParam.getLayoutWidth(),
                        localVideoParam.getLayoutHeight());
                localVideoView.setScalingType(localVideoParam.getScalingType());
            } else {
                localContainerLayout.setPosition(       //connecting view.
                        localVideoParam.getLayoutPositionX(), localVideoParam.getLayoutPositionY(), localVideoParam.getLayoutWidth(),
                        localVideoParam.getLayoutHeight());
                localVideoView.setScalingType(localVideoParam.getScalingType());
            }
            localVideoView.setMirror(localVideoParam.isMirror());
            localVideoView.setZOrderMediaOverlay(localVideoParam.iszOrderMediaOverlay());

            localVideoView.requestLayout();
        }
    }

    private void updateRemoteVideoView() {
        if (remoteVideoView != null) {
            remoteContainerLayout.setPosition(remoteVideoParam.getLayoutPositionX(), remoteVideoParam.getLayoutPositionY(), remoteVideoParam.getLayoutWidth(), remoteVideoParam.getLayoutHeight());
            remoteVideoView.setScalingType(remoteVideoParam.getScalingType());
            remoteVideoView.setMirror(remoteVideoParam.isMirror());
            remoteVideoView.setZOrderMediaOverlay(remoteVideoParam.iszOrderMediaOverlay());
            remoteVideoView.requestLayout();
        }
    }

    public void attchLocalRenderer(PercentFrameLayout localContainerLayout, VideoViewParam localVideoParam, SurfaceViewRenderer localVideoView) {
        this.localContainerLayout = localContainerLayout;
        this.localVideoParam = localVideoParam;
        this.localVideoView = localVideoView;

//        localVideoView.init(eglBase.getContext(), new PVideoRenderEvents(localVideoView));
        peer.addLocalRenderer(localVideoView);

        updateLocalVideoView();

//
    }

    public void attachRemoteRenderer(PercentFrameLayout remoteContainerLayout, VideoViewParam remoteVideoParam, SurfaceViewRenderer remoteVideoView) {
        this.remoteContainerLayout = remoteContainerLayout;
        this.remoteVideoParam = remoteVideoParam;
        this.remoteVideoView = remoteVideoView;

        peer.addRemoteRender(remoteVideoView);

        updateRemoteVideoView();
    }

    public void dispatchLocalRenderer(SurfaceViewRenderer surfaceViewRenderer) {
//        peer.setVideoEnabled(false);
        if (peer != null)
            peer.removeLocalRenderer(surfaceViewRenderer);
    }

    public void dispatchRemoteRenderer(SurfaceViewRenderer surfaceViewRenderer) {
        if (peer != null)
            peer.removeRemoteRenderer(surfaceViewRenderer);
    }

    public void initPeerConnectionFactory(PeerConnectionFactory.Options options, KPeerConnectionClient.PeerConnectionParameters parameters) {


        peer.createPeerConnectionFactory(context, parameters, this);
        peer.setPeerConnectionFactoryOptions(options);

    }

    public void createPeerConnection(
            List<PeerConnection.IceServer> iceServers) {

        peer.createPeerConnection(eglBase.getContext(), connectionType, iceServers);
    }

    public void startCall() {

        peer.createOffer();
    }

    private void callConnected() {

        participantEvents.onIceConnected(name);
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KPeerConnectionClient getPeer() {
        return peer;
    }

    public void setPeer(KPeerConnectionClient peer) {
        this.peer = peer;
    }

    @Override
    public void onIceConnected() {
        iceConnected = true;
        LogCat.i("Participant " + name + " ice connected.");

        SinglExecterPool.getIntance().execute(new Runnable() {
            @Override
            public void run() {
                callConnected();
            }
        });

    }

    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        LogCat.debug("Participant " + name + " onLocalDescription create. : " + sdp);
        SinglExecterPool.getIntance().execute(new Runnable() {
            @Override
            public void run() {
                participantEvents.onOfferCreated(name, sdp.description);
            }
        });

    }

    @Override
    public void onLocalIceCandidate(final IceCandidate candidate) {
        LogCat.debug("Participant " + name + " onLocalIceCandidate create. : " + candidate);
        SinglExecterPool.getIntance().execute(new Runnable() {
            @Override
            public void run() {
                participantEvents.onLocalIceCandidate(name, candidate);
            }
        });

    }

    @Override
    public void onIceDisconnected() {

        LogCat.debug("Participant " + name + " ice disConnected : ");
        SinglExecterPool.getIntance().execute(new Runnable() {
            @Override
            public void run() {
                participantEvents.onPeerDisconnected(name);
                disconnectPeer();
            }
        });

    }

    @Override
    public void onPeerConnectionClosed() {
        LogCat.debug("Participant " + name + " ice disConnected : ");
        SinglExecterPool.getIntance().execute(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    /**
     * Disconnect from remote resources, dispose of local resources, and exit.
     */
    public void disconnectPeer() {

        if (localVideoView != null) {
            localVideoView.release();
            localVideoView = null;
        }
        if (remoteVideoView != null) {
            remoteVideoView.release();
            remoteVideoView = null;
        }
        if (peer != null) {
            LogCat.debug("Participant " + name + " peer disconnect : ");
            iceConnected = false;
            peer.close();
            peer = null;
        }

    }

    public void stopVideo() {
        if (iceConnected) {
            peer.stopVideoSource();
        }
    }

    public void startVideo() {

        if (iceConnected) {
            peer.startVideoSource();
        }
    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {

        LogCat.i(name + " peer is ready : " + reports);
    }

    @Override
    public void onPeerConnectionError(final String description) {

        SinglExecterPool.getIntance().execute(new Runnable() {
            @Override
            public void run() {
                participantEvents.onParticipantPeerError(name, description);
                LogCat.e("participant " + name + " error : " + description);
                disconnectPeer();
            }
        });
    }

    public SurfaceViewRenderer getLocalVideoView() {
        return localVideoView;
    }

    public void setLocalVideoView(SurfaceViewRenderer localVideoView) {
        this.localVideoView = localVideoView;
    }

    public SurfaceViewRenderer getRemoteVideoView() {
        return remoteVideoView;
    }

    public void setRemoteVideoView(SurfaceViewRenderer remoteVideoView) {
        this.remoteVideoView = remoteVideoView;
    }

    public PercentFrameLayout getLocalContainerLayout() {
        return localContainerLayout;
    }

    public void setLocalContainerLayout(PercentFrameLayout localContainerLayout) {
        this.localContainerLayout = localContainerLayout;
    }

    public PercentFrameLayout getRemoteContainerLayout() {
        return remoteContainerLayout;
    }

    public void setRemoteContainerLayout(PercentFrameLayout remoteContainerLayout) {
        this.remoteContainerLayout = remoteContainerLayout;
    }

    public ParticipantEvents getParticipantEvents() {
        return participantEvents;
    }

    public void setParticipantEvents(ParticipantEvents participantEvents) {
        this.participantEvents = participantEvents;
    }

    public EglBase getEglBase() {
        return eglBase;
    }

    public boolean isIceConnected() {
        return iceConnected;
    }

    public interface ParticipantEvents {

        public void onOfferCreated(String name, String sdp);

        public void onLocalIceCandidate(String name, IceCandidate candidate);

        public void onPeerDisconnected(String name);

        public void onIceConnected(String name);

        public void onParticipantPeerError(String name, String msg);

    }
}
