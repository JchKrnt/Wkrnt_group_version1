package com.sohu.jch.krnt_android_group.view;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.sohu.jch.krnt_android_group.R;
import com.sohu.jch.krnt_android_group.controller.KGroupSocketClient;
import com.sohu.jch.krnt_android_group.util.Constants;
import com.sohu.jch.krnt_android_group.util.SharePrefUtil;

/**
 * Created by jingbiaowang on 2015/12/3.
 */
public class KGroupApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        wsConnect();
    }


    public void wsConnect() {

        KGroupSocketClient groupSocketClient = KGroupSocketClient.getInstance();
        groupSocketClient.connect(SharePrefUtil.getInstance(getApplicationContext(), R.xml.setting_pref).getWebSocketUrl(), connectEvents);

    }

    private KGroupSocketClient.KGroupSocketConnectEvents connectEvents = new KGroupSocketClient.KGroupSocketConnectEvents() {
        @Override
        public void onSocketConnected() {
            new Handler(getApplicationContext().getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Toast.makeText(getApplicationContext(), "网络连接成功。", Toast.LENGTH_SHORT).show();
                }
            }.sendEmptyMessage(0);
        }

        @Override
        public void onSocketDisConnected(String msg) {

            new Handler(getApplicationContext().getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Toast.makeText(getApplicationContext(), "已经断开网络连接。", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Constants.SOCKET_RECEIVER_FILTER);
                    intent.putExtra("data", Constants.SOCKET_ERRO);
                    sendBroadcast(intent);
                }
            }.sendEmptyMessage(0);

        }

        @Override
        public void onSockError(final String error) {

            new Handler(getApplicationContext().getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Toast.makeText(getApplicationContext(), "网络连接失败。－－请重新连接。" + error, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Constants.SOCKET_RECEIVER_FILTER);
                    intent.putExtra("data", Constants.SOCKET_ERRO);
                    sendBroadcast(intent);
                }
            }.sendEmptyMessage(0);

        }
    };
}
