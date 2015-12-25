package com.sohu.jch.krnt_android_group.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sohu.jch.krnt_android_group.model.ExistParticipantMsgBean;
import com.sohu.jch.krnt_android_group.model.GroupSocketMsgBean;
import com.sohu.jch.krnt_android_group.model.ParticipantMsgBean;
import com.sohu.jch.krnt_android_group.model.ParticipantSdpMsgBean;
import com.sohu.kurento.bean.IceCandidate;
import com.sohu.kurento.netClient.WebSocketChannel;
import com.sohu.kurento.util.LogCat;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by jingbiaowang on 2015/12/3.
 */
public class KGroupSocketClient implements Closeable, WebSocketChannel.WebSocketEvents {

    private static final String TAG = "KGroupSocketClient : ";

    public interface KGroupSocketConnectEvents {

        public void onSocketConnected();

        public void onSocketDisConnected(String msg);

        public void onSockError(String error);

    }

    public interface KGroupInternalEvents {

        public void onExistingParticipants(ExistParticipantMsgBean existParticipantMsgBean);

        public void onNewParticipants(ParticipantMsgBean participantMsgBean);

        public void onParticipantLeft(ParticipantMsgBean participantMsgBean);

        public void onRemoteIceCandidate(ParticipantSdpMsgBean participantSdpMsgBean);

        public void onReceiveSdpAnswer(ParticipantSdpMsgBean sdpMsgBean);

    }

    private static KGroupSocketClient instance;
    private static WebSocketChannel socketChannel;

    private KGroupSocketConnectEvents connectEvents;

    private KGroupInternalEvents internalEvents;

    Gson gson = new Gson();

    public static KGroupSocketClient getInstance() {

        if (instance == null) {
            instance = new KGroupSocketClient();
        }

        return instance;
    }

    private KGroupSocketClient() {

        socketChannel = new WebSocketChannel();
//        socketChannel.setExecutor(executor);
    }

    public void connect(final String url, KGroupSocketConnectEvents events) {

        this.connectEvents = events;
        socketChannel.connect(url, KGroupSocketClient.this);
    }

    public KGroupSocketConnectEvents getConnectEvents() {
        return connectEvents;
    }

    public void setConnectEvents(KGroupSocketConnectEvents connectEvents) {
        this.connectEvents = connectEvents;
    }

    public KGroupInternalEvents getInternalEvents() {
        return internalEvents;
    }

    public void setInternalEvents(KGroupInternalEvents internalEvents) {
        this.internalEvents = internalEvents;
    }

    @Override
    public void close() throws IOException {

        if (socketChannel != null) {
            socketChannel.disconnect(true);
            socketChannel = null;
        }

        instance = null;
    }

    /**
     * WebSocketEvents.
     *
     * @param e
     */
    @Override
    public void onError(final String e) {
        connectEvents.onSockError(e);
    }

    /**
     * WebSocketEvents.
     */
    @Override
    public void onConnected() {
        connectEvents.onSocketConnected();
    }

    /**
     * WebSocketEvents.
     *
     * @param msg
     */
    @Override
    public void onMessage(String msg) {
        LogCat.v("receive socket messag on Client : " + msg);
        GroupSocketMsgBean msgBean = gson.fromJson(msg, GroupSocketMsgBean.class);
        switch (msgBean.getId()) {
            case existingParticipants: {

                final ExistParticipantMsgBean existParticipantMsgBean = gson.fromJson(msg, ExistParticipantMsgBean.class);
                internalEvents.onExistingParticipants(existParticipantMsgBean);

                break;
            }

            case newParticipantArrived: {

                final ParticipantMsgBean participantMsgBean = gson.fromJson(msg, ParticipantMsgBean.class);
                internalEvents.onNewParticipants(participantMsgBean);

                break;
            }

            case participantLeft: {

                final ParticipantMsgBean participantMsgBean = gson.fromJson(msg, ParticipantMsgBean.class);
                internalEvents.onParticipantLeft(participantMsgBean);

                break;
            }

            case receiveVideoAnswer: {

                final ParticipantSdpMsgBean participantSdpMsgBean = gson.fromJson(msg, ParticipantSdpMsgBean.class);
                internalEvents.onReceiveSdpAnswer(participantSdpMsgBean);

                break;
            }

            case iceCandidate: {
                final ParticipantSdpMsgBean participantSdpMsgBean = gson.fromJson(msg, ParticipantSdpMsgBean.class);
                internalEvents.onRemoteIceCandidate(participantSdpMsgBean);

                break;
            }
        }

    }

    /**
     * WebSocketEvents
     *
     * @param msg
     */
    @Override
    public void onClosed(String msg) {
        connectEvents.onSocketDisConnected(msg);
    }

    public void register(String name, String roomName) {

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "joinRoom");
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("room", roomName);

        socketChannel.sendMsg(gson.toJson(jsonObject));
    }

    public void sendOffer(String name, String offer) {
        LogCat.v(TAG + "sendOffer for " + name + " offer: " + offer);
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "receiveVideoFrom");
        jsonObject.addProperty("sender", name);
        jsonObject.addProperty("sdpOffer", offer);
        socketChannel.sendMsg(gson.toJson(jsonObject));

    }

    public void sendLocalIceCandidate(String name, IceCandidate iceCandidate) {
        LogCat.v(TAG + "sendLocalIceCandidate for " + name + " iceCandidate : " + iceCandidate);
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "onIceCandidate");
        jsonObject.add("candidate", gson.toJsonTree(iceCandidate));
        jsonObject.addProperty("name", name);

        socketChannel.sendMsg(gson.toJson(jsonObject));
    }

    public void leaveRoom() {

        LogCat.v(TAG + "leaveRoom for.");
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "leaveRoom");

        socketChannel.sendMsg(gson.toJson(jsonObject));
    }

    public boolean isOpened() {
        return !socketChannel.isClosed();
    }
}
