package com.sohu.jch.krnt_android_group.model;

import java.util.ArrayList;

/**
 * Created by jingbiaowang on 2015/12/3.
 */
public class ExistParticipantMsgBean extends GroupSocketMsgBean {

    private ArrayList<String> data;

    /**
     * names
     *
     * @return
     */
    public ArrayList<String> getData() {
        return data;
    }

    public void setData(ArrayList<String> data) {
        this.data = data;
    }
}
