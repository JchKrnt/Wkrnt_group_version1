package com.sohu.kurento.bean;

/**
 * Created by jingbiaowang on 2015/7/23.
 */
public enum UserType {

    PRESENTER, VIEWER, BOTH;

    public String getVauleStr() {
        return name().toLowerCase();
    }

    public static UserType fromCanonicalForm(String canonical) {
        return UserType.valueOf(UserType.class, canonical.toUpperCase());
    }
}
