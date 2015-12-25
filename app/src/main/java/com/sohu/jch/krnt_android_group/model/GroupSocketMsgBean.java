package com.sohu.jch.krnt_android_group.model;

/**
 * Created by jingbiaowang on 2015/12/3.
 * <p/>
 * Base bean.
 */
public class GroupSocketMsgBean {

    public enum SocketBeanIdType {

        existingParticipants, newParticipantArrived, participantLeft, receiveVideoAnswer, iceCandidate
    }

    protected SocketBeanIdType id;

    public SocketBeanIdType getId() {
        return id;
    }

    public void setId(SocketBeanIdType id) {
        this.id = id;
    }
}
