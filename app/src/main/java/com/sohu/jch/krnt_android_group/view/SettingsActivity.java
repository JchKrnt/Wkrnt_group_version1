package com.sohu.jch.krnt_android_group.view;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.sohu.jch.krnt_android_group.R;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {

    private MySettingPreferenceFragmet fragmet;
    private MySharePreferenceChangeListener sharePreferenceChangeListener = new
            MySharePreferenceChangeListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fManager = getFragmentManager();
        FragmentTransaction ft = fManager.beginTransaction();
        fragmet = new MySettingPreferenceFragmet();
        FragmentTransaction fragmentTransaction = ft.replace(android.R.id.content, fragmet);
//        fragmentTransaction.commit();
        ft.commit();

    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = fragmet.getPreferenceScreen().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharePreferenceChangeListener);

        initSharePreference(sharedPreferences);
    }

    private void initSharePreference(SharedPreferences sharedPreferences) {

        updateSummary(sharedPreferences, getString(R.string.pref_key_host));
        updateSummary(sharedPreferences, getString(R.string.pref_key_port));
        updateSummary(sharedPreferences, getString(R.string.pref_key_method));
        updateSummary(sharedPreferences, getString(R.string.pref_key_stun));

        updateCheckSummary(sharedPreferences, getString(R.string.pref_key_video_callable));
    }

    private void updateSummary(SharedPreferences sharedPreferences, String key) {

        Preference updatePref = fragmet.findPreference(key);
        updatePref.setSummary(sharedPreferences.getString(key, ""));
    }

    private void updateCheckSummary(SharedPreferences preferences, String key) {

        CheckBoxPreference updatePref = (CheckBoxPreference) fragmet.findPreference(key);
        updatePref.setChecked(preferences.getBoolean(key, true));
    }

    @Override
    protected void onPause() {
        fragmet.getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(sharePreferenceChangeListener);
        super.onPause();

    }

    /**
     * fragment.
     */
    public static class MySettingPreferenceFragmet extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.setting_pref);
        }
    }


    private class MySharePreferenceChangeListener implements SharedPreferences
            .OnSharedPreferenceChangeListener {


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            if (key.equals(getString(R.string.pref_key_host)) ||
                    key.equals(getString(R.string.pref_key_port)) ||
                    key.equals(getString(R.string.pref_key_method)) ||
                    key.equals(getString(R.string.pref_key_stun))) {

                updateSummary(sharedPreferences, key);
            } else if (key.equals(getString(R.string.pref_key_video_callable))) {

                updateCheckSummary(sharedPreferences, getString(R.string.pref_key_video_callable));
            }

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                setResult(RESULT_OK);
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
