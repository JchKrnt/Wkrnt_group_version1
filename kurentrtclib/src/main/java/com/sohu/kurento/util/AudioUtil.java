package com.sohu.kurento.util;

import android.content.Context;
import android.media.AudioManager;

/**
 * Created by jingbiaowang on 2015/7/30.
 */
public class AudioUtil {

    private static int currVolume = 0;

    //打开扬声器
    public static void OpenSpeaker(Context context) {
        try {

            //判断扬声器是否在打开
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            audioManager.setBluetoothScoOn(true);
            //获取当前通话音量
            currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

            if (!audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(true);

                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.STREAM_VOICE_CALL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //关闭扬声器
    public static void CloseSpeaker(Context context) {

        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currVolume,
                            AudioManager.STREAM_VOICE_CALL);
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Toast.makeText(context,扬声器已经关闭",Toast.LENGTH_SHORT).show();
    }
}
