package com.sohu.jch.krnt_android_group.view.play;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.sohu.jch.krnt_android_group.R;
import com.sohu.jch.krnt_android_group.controller.Participant;
import com.sohu.jch.krnt_android_group.controller.RoomController;
import com.sohu.jch.krnt_android_group.util.Constants;
import com.sohu.jch.krnt_android_group.util.DividerItemDecoration;
import com.sohu.jch.krnt_android_group.util.SharePrefUtil;
import com.sohu.kurento.client.AppRTCAudioManager;
import com.sohu.kurento.group.KPeerConnectionClient;
import com.sohu.kurento.util.LogCat;
import com.sohu.kurento.util.SinglExecterPool;

public class PlayActivity extends AppCompatActivity implements RoomController.RoomControllerViewEvents, View.OnClickListener {

    private RecyclerView recview;
    private VideoRecycleAdapter adapter;
    private RoomController roomController;
    private boolean isActivitying = false;
    private Button closebtn;
    private TextView nameTv;

    private PlayBroadCastReceiver receiver = null;
    private AppRTCAudioManager audioManager = null;

    private AudioManager sysAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Thread.setDefaultUncaughtExceptionHandler(
//                new UnhandledExceptionHandler(this));
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        sysAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        initialize();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, new IntentFilter(Constants.SOCKET_RECEIVER_FILTER));
        audioManager.init();
        roomController.joinRoom(getIntent().getStringExtra("name"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivitying = true;
        roomController.startVideo();

    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
        roomController.stopVideo();
        audioManager.close();
    }

    @Override
    protected void onStop() {
        isActivitying = false;

        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_btn: {
                closeRoom();
//                adapter.notifyInsertedParticipant(adapter.getItemCount(), "participant " + adapter.getItemCount());
                break;
            }
        }
    }

    private void initialize() {
        LogCat.debug("--- before init view.");
        closebtn = (Button) findViewById(R.id.close_btn);
        recview = (RecyclerView) findViewById(R.id.rec_view);
        nameTv = (TextView) findViewById(R.id.room_name_tv);
        adapter = new VideoRecycleAdapter(getApplicationContext());
        recview.setAdapter(adapter);
        recview.setHasFixedSize(true);
        recview.setLayoutManager(new GridLayoutManager(getApplicationContext(), 2, LinearLayoutManager.VERTICAL, false));
        recview.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL_LIST));

        nameTv.setText(getIntent().getStringExtra("roomName"));
        LogCat.debug("--- after nit view.");

        roomController = new RoomController(getApplicationContext(), getIntent().getStringExtra("roomName"));
        roomController.setControllerEvents(this);

        closebtn.setOnClickListener(this);

        audioManager = AppRTCAudioManager.create(this, new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                }
        );

        receiver = new PlayBroadCastReceiver();
    }

    /**********************
     * RoomControllerViewEvents
     ***********************/
    @Override
    public KPeerConnectionClient.PeerConnectionParameters getPeerConnectionParam() {

        KPeerConnectionClient.PeerConnectionParameters peerConnectionParameters = new KPeerConnectionClient.PeerConnectionParameters(SharePrefUtil.getInstance(getApplicationContext(), R.xml.setting_pref).getVedioAble(),
                false, 0, 0, 18, 200, KPeerConnectionClient.VIDEO_CODEC_VP8, true, 128, KPeerConnectionClient.AUDIO_CODEC_OPUS, false, true);

        return peerConnectionParameters;
    }


    @Override
    public void onParticipantJoined(final int position, final Participant participant) {
        LogCat.i("recycle view add participant for : " + participant.getName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyInsertedParticipant(position, participant);
            }
        });

    }

    @Override
    public void onReportError(final String ParName, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String erroMsg = "participant : " + ParName + " has error : " + msg;
                if (isActivitying) {
                    AlertDialog alertDialog = new AlertDialog.Builder(PlayActivity.this).setTitle("Error!").setIcon(android.R.drawable.stat_notify_error).setMessage(erroMsg).create();
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            PlayActivity.this.finish();
                        }
                    });
                    alertDialog.show();
                }

                LogCat.e(erroMsg);
            }
        });

    }

    @Override
    public void onRemoveParticipant(final int index, final Participant participant) {
        LogCat.i("recycle view remove participant for : " + participant.getName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyRemovedParticipant(index, participant);
            }
        });

    }

    @Override
    public void onVideoConnected(final int position, final Participant participant) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogCat.i("recycle view update View on video connected for : " + participant.getName());
                View itemView = recview.getChildAt(position);
                if (itemView != null) {
                    RecyclerView.ViewHolder viewHolder = recview.getChildViewHolder(itemView);
                    if (viewHolder != null && viewHolder instanceof VideoRecycleAdapter.VideoHolder)
                        ((VideoRecycleAdapter.VideoHolder) viewHolder).videopro.setVisibility(View.GONE);
                }
                participant.updateVideoView();
            }
        });
    }

    @Override
    public void onRoomExists(String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setResult(RESULT_OK);
                finish();
            }
        });

    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.

    }

    public class PlayBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            int id = intent.getIntExtra("data", 0);
            if (id == Constants.SOCKET_ERRO) {

//                PlayActivity.this.setResult(RESULT_OK);
//                PlayActivity.this.finish();
            }
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {

            case KeyEvent.KEYCODE_VOLUME_DOWN: {

                sysAudioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_LOWER, AudioManager.STREAM_VOICE_CALL, 0);
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_UP: {
                sysAudioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE, AudioManager.STREAM_VOICE_CALL, 0);
                return true;
            }

            case KeyEvent.KEYCODE_BACK: {
                closeRoom();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void closeRoom() {
        roomController.exitRoom();
//        PlayActivity.this.setResult(RESULT_OK);
//        PlayActivity.this.finish();
        SinglExecterPool.getIntance().shutdown();
    }
}
