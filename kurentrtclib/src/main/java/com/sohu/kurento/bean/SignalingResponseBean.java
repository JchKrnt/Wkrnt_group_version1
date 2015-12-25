package com.sohu.kurento.bean;


import java.util.ArrayList;

/**
 * Created by jingbiaowang on 2015/8/28.
 */
public class SignalingResponseBean {

    /**
     * the repones id type in the parameter.
     */
    public enum ResponeIdType {
        roomList, register, iceCandidate, viewerResponse, presenterResponse, stopCommunication
    }

    /**
     * the response type in the parameter.
     */
    public enum ResponseType {
        accepted, rejected
    }

    private String id;
    private String response;
    //response data for failure request.
    private String message;
    //response data after register success.
    private RoomBean room;
    //response data after ice request.
    private IceCandidate candidate;
    //response data for sdp interact.
    private String sdpAnswer;
    //response data after room list request.
    private ArrayList<RoomBean> roomsList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public IceCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(IceCandidate candidate) {
        this.candidate = candidate;
    }

    public String getSdpAnswer() {
        return sdpAnswer;
    }

    public void setSdpAnswer(String sdpAnswer) {
        this.sdpAnswer = sdpAnswer;
    }


    public ArrayList<RoomBean> getRoomsList() {
        return roomsList;
    }

    public RoomBean getRoom() {
        return room;
    }

    public void setRoom(RoomBean room) {
        this.room = room;
    }

    public void setRoomsList(ArrayList<RoomBean> roomsList) {
        this.roomsList = roomsList;
    }


}
