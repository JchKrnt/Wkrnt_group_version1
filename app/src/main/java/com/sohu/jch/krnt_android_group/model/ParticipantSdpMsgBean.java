package com.sohu.jch.krnt_android_group.model;

import com.sohu.kurento.bean.IceCandidate;

/**
 * Created by jingbiaowang on 2015/12/3.
 * <p/>
 * sdp ,answer, candidate.
 */
public class ParticipantSdpMsgBean extends ParticipantMsgBean {

    private String sdpAnswer;

    private IceCandidate candidate;


    public String getSdpAnswer() {

        return sdpAnswer;
    }

    public void setSdpAnswer(String sdpAnswer) {
        this.sdpAnswer = sdpAnswer;
    }

    public IceCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(IceCandidate candidate) {
        this.candidate = candidate;
    }
}
