package com.sohu.kurento.client;

import org.webrtc.IceCandidate;

/**
 * Created by jingbiaowang on 2015/7/24.
 */
public interface KWSessionEvent {

    void createOffer();

    public void addRemoteCandidate(IceCandidate candidate);

    void setRemoteSdp(String anwser);


    /**
     * stop.
     */
    public void dispose();

}
