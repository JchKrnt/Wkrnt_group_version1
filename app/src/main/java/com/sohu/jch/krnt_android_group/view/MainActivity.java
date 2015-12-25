package com.sohu.jch.krnt_android_group.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sohu.jch.krnt_android_group.R;
import com.sohu.jch.krnt_android_group.controller.KGroupSocketClient;
import com.sohu.jch.krnt_android_group.view.play.PlayActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText roomet;
    private EditText nameet;
    private Button registerbtn;
    private Button connBtn;

    private static final int SETTING_INTENT = 434;
    private static final int PLAY_INTENT = 435;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    private void initialize() {

        roomet = (EditText) findViewById(R.id.room_et);
        nameet = (EditText) findViewById(R.id.name_et);
        registerbtn = (Button) findViewById(R.id.register_btn);
        connBtn = (Button) findViewById(R.id.conn_btn);

        registerbtn.setOnClickListener(this);
        connBtn.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.settings, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.menu_set) {

            startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), SETTING_INTENT);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.register_btn: {

                String roomNameStr = roomet.getText().toString().trim();
                String nameStr = nameet.getText().toString().trim();
                if ("".equals(roomNameStr) || "".equals(nameStr)) {
                    Toast.makeText(getApplicationContext(), "请输入你的昵称和房间名！", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                    intent.putExtra("name", nameStr);
                    intent.putExtra("roomName", roomNameStr);
                    startActivityForResult(intent, PLAY_INTENT);
                }
                break;
            }
            case R.id.conn_btn: {

                if (!KGroupSocketClient.getInstance().isOpened()) {
                    KGroupApp app = (KGroupApp) getApplication();
                    app.wsConnect();
                }
                break;

            }


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTING_INTENT && resultCode == RESULT_OK) {

        } else if (requestCode == PLAY_INTENT && resultCode == RESULT_OK) {


        }

    }
}
