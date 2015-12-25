package com.sohu.kurento.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jingbiaowang on 2015/8/31.
 */
public class RoomBean implements Parcelable {

    private String sessionId;
    private String name;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.sessionId);
        dest.writeString(this.name);
    }

    public RoomBean() {
    }

    protected RoomBean(Parcel in) {
        this.sessionId = in.readString();
        this.name = in.readString();
    }

    public static final Parcelable.Creator<RoomBean> CREATOR = new Parcelable.Creator<RoomBean>() {
        public RoomBean createFromParcel(Parcel source) {
            return new RoomBean(source);
        }

        public RoomBean[] newArray(int size) {
            return new RoomBean[size];
        }
    };
}
