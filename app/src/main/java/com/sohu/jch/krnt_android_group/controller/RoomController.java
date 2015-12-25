package com.sohu.jch.krnt_android_group.controller;

import android.content.Context;

import com.sohu.jch.krnt_android_group.model.ExistParticipantMsgBean;
import com.sohu.jch.krnt_android_group.model.ParticipantMsgBean;
import com.sohu.jch.krnt_android_group.model.ParticipantSdpMsgBean;
import com.sohu.kurento.group.KPeerConnectionClient;
import com.sohu.kurento.util.LogCat;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by jingbiaowang on 2015/12/4.
 */
public class RoomController implements KGroupSocketClient.KGroupInternalEvents, Participant.ParticipantEvents {

    private String roomName;

    private String localParName;

    private Participant register;

    private Context context;

    private ConcurrentMap<String, Participant> participants = new ConcurrentHashMap<>();

    private EglBase eglBase;

    private KGroupSocketClient socketClient;

    public RoomControllerViewEvents controllerEvents;

    public interface RoomControllerViewEvents {


        public KPeerConnectionClient.PeerConnectionParameters getPeerConnectionParam();

        public void onParticipantJoined(int position, Participant paticipant);

        public void onReportError(String ParName, String msg);

        public void onRemoveParticipant(int index, Participant participant);

        public void onVideoConnected(int positioin, Participant participant);

        public void onRoomExists(String name);

    }

    public RoomController(Context context, String roomName) {
        this.context = context;
        this.roomName = roomName;
        eglBase = new EglBase();

        socketClient = KGroupSocketClient.getInstance();
        socketClient.setInternalEvents(this);
    }

    public void joinRoom(String participantName) {
        this.localParName = participantName;
        socketClient.register(participantName, roomName);

    }

    public Participant createParticipant(String name, KPeerConnectionClient.ConnectionType connectionType) {

        Participant participant = new Participant(context, eglBase, name, connectionType);

        LogCat.i(" participant :" + name + " was created");
        participant.setParticipantEvents(this);

        synchronized (participants) {
            participants.put(participant.getName(), participant);

        }
        // update view. create a video view.
        controllerEvents.onParticipantJoined(participants.size() - 1, participant);

        initParticipantPeer(participant, participants.size() - 1);

        return participant;

    }

    private void initParticipantPeer(Participant participant, int position) {
//        if (participant.getConnectionType() == KPeerConnectionTypeBean.ConnectionType.SEND_ONLY) {
//            participant.setLocalVideo(controllerEvents.getLocalVideo(position).getVideoViewContainer(), controllerEvents.getLocalVideoViewParam(position), controllerEvents.getLocalVideo(position).getVideoView());
//        } else if (participant.getConnectionType() == KPeerConnectionTypeBean.ConnectionType.READ_ONLY) {
//            participant.attachRemoteRenderer(controllerEvents.getRemoteVideo(position).getVideoViewContainer(), controllerEvents.getRemoteVideoViewParam(position), controllerEvents.getRemoteVideo(position).getVideoView());
//
//        } else {
//            participant.setLocalVideo(controllerEvents.getLocalVideo(position).getVideoViewContainer(), controllerEvents.getLocalVideoViewParam(position), controllerEvents.getLocalVideo(position).getVideoView());
//            participant.attachRemoteRenderer(controllerEvents.getRemoteVideo(position).getVideoViewContainer(), controllerEvents.getRemoteVideoViewParam(position), controllerEvents.getRemoteVideo(position).getVideoView());
//        }
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = PeerConnectionFactory.Options.ADAPTER_TYPE_WIFI;
        participant.initPeerConnectionFactory(options, controllerEvents.getPeerConnectionParam());
        participant.createPeerConnection(getServers());

    }


    public List<PeerConnection.IceServer> getServers() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
        iceServers.add(new PeerConnection.IceServer("stun:123.126.104.237:4000"));
        iceServers.add(new PeerConnection.IceServer("turn:123.126.104.47:3478", "dooler", "123456"));

        return iceServers;
    }

    public void exitRoom() {

        socketClient.leaveRoom();

        try {
            socketClient.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogCat.e(" socket close error " + e.getMessage());
        }

        for (Participant participant :
                participants.values()) {
            participant.disconnectPeer();
        }

    }
/**************socket events.***************/

    /**
     * @param existParticipantMsgBean
     */
    @Override
    public void onExistingParticipants(final ExistParticipantMsgBean existParticipantMsgBean) {

        LogCat.debug("onExistingParticipants create: " + localParName);
        //create local participant.
        Participant localParticipant = createParticipant(localParName, KPeerConnectionClient.ConnectionType.SEND_ONLY);
        RoomController.this.register = localParticipant;
        localParticipant.startCall();

        //create remote participant.
        for (String name : existParticipantMsgBean.getData()) {
            LogCat.debug("onExistingParticipants create: " + name);
            createParticipant(name, KPeerConnectionClient.ConnectionType.READ_ONLY).startCall();

        }


    }

    @Override
    public void onNewParticipants(ParticipantMsgBean participantMsgBean) {
        LogCat.debug("--- new participant ： " + participantMsgBean.getName());
        createParticipant(participantMsgBean.getName(), KPeerConnectionClient.ConnectionType.READ_ONLY).startCall();
    }

    @Override
    public void onParticipantLeft(ParticipantMsgBean participantMsgBean) {

        LogCat.debug("--- onParticipantLeft ： " + participantMsgBean.getName());
        Participant participant = participants.get(participantMsgBean.getName());
        ArrayList<String> keyList = new ArrayList<>(participants.keySet());
        int position = keyList.indexOf(participantMsgBean.getName());
        controllerEvents.onRemoveParticipant(position, participant);

        participant.disconnectPeer();

    }

    @Override
    public void onRemoteIceCandidate(ParticipantSdpMsgBean participantSdpMsgBean) {

        com.sohu.kurento.bean.IceCandidate kIceCandidate = participantSdpMsgBean.getCandidate();
        participants.get(participantSdpMsgBean.getName()).getPeer().addRemoteIceCandidate(new IceCandidate(kIceCandidate.sdpMid, kIceCandidate.sdpMLineIndex, kIceCandidate.candidate));
    }

    @Override
    public void onReceiveSdpAnswer(ParticipantSdpMsgBean sdpMsgBean) {

        participants.get(sdpMsgBean.getName()).getPeer().setRemoteDescription(new SessionDescription(SessionDescription.Type.ANSWER, sdpMsgBean.getSdpAnswer()));

    }

    /**************peerConnection events.***************/
    /**
     * @param name
     * @param sdp
     */
    @Override
    public void onOfferCreated(String name, String sdp) {
        socketClient.sendOffer(name, sdp);
    }

    @Override
    public void onLocalIceCandidate(String name, IceCandidate candidate) {

        socketClient.sendLocalIceCandidate(name, new com.sohu.kurento.bean.IceCandidate(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex));
    }

    @Override
    public void onPeerDisconnected(String name) {

        synchronized (participants) {
            Participant participant = participants.remove(name);
            if (participant != null && participants.size() == 0) {
                controllerEvents.onRoomExists(localParName);
            }
        }

    }

    @Override
    public void onIceConnected(String name) {
        synchronized (participants) {
            ArrayList<String> keyList = new ArrayList<>(participants.keySet());
            int position = keyList.indexOf(name);

            controllerEvents.onVideoConnected(position, participants.get(name));
        }
    }

    @Override
    public void onParticipantPeerError(String name, String msg) {

        controllerEvents.onReportError(name, msg);
    }

    public Set<String> getParticipantNames() {

        return participants.keySet();
    }

    public Participant getRegister() {
        return register;
    }

    public void setRegister(Participant register) {
        this.register = register;
    }

    public RoomControllerViewEvents getControllerEvents() {
        return controllerEvents;
    }

    public void setControllerEvents(RoomControllerViewEvents controllerEvents) {
        this.controllerEvents = controllerEvents;
    }

    public void stopVideo() {
        for (Participant participant :
                participants.values()) {
            participant.stopVideo();
        }
    }

    public void startVideo() {
        for (Participant participant :
                participants.values()) {
            participant.startVideo();
        }
    }
}
