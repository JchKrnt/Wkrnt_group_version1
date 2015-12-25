package com.sohu.jch.krnt_android_group.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sohu.jch.krnt_android_group.R;


/**
 * Created by jingbiaowang on 2015/11/17.
 */
public class SharePrefUtil {

    private Context context;

    private static SharePrefUtil instance;

    private SharedPreferences preferences;

    public static SharePrefUtil getInstance(Context context, int preSrc) {

        if (instance == null) {
            instance = new SharePrefUtil(context, preSrc);
        }
        return instance;
    }

    public SharePrefUtil(Context context, int preSrc) {
        this.context = context;
        PreferenceManager.setDefaultValues(context, preSrc, false);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public String getWebSocketUrl() {

        String host = preferences.getString(context.getString(R.string.pref_key_host), context.getString(R.string.pref_default_host));
        String port = preferences.getString(context.getString(R.string.pref_key_port), context.getString(R.string.pref_default_port));
        String method = preferences.getString(context.getString(R.string.pref_key_method), context.getString(R.string.pref_default_method));

        StringBuffer urlSb = new StringBuffer(host);
        urlSb.append(":").append(port).append("/").append(method);

        return urlSb.toString();
    }


    public boolean getVedioAble() {
        return preferences.getBoolean(context.getString(R.string.pref_key_video_callable), true);
    }

}
