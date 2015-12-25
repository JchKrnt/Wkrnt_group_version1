package com.sohu.kurento.bean;

/**
 * Created by jingbiaowang on 2015/9/9.
 */
public class IceCandidate {
    public String candidate;
    public String sdpMid;
    public int sdpMLineIndex;

    public IceCandidate(String candidate, String sdpMid, int sdpMLineIndex) {
        this.candidate = candidate;
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
    }


}
