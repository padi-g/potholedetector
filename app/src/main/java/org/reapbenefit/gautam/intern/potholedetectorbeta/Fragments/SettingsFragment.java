package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs = getContext().getSharedPreferences("uploads",MODE_PRIVATE);
        prefsEditor = prefs.edit();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        boolean autoUploadOption = prefs.getBoolean(getString(R.string.auto_upload_setting), true);
        if (s.equals(getString(R.string.auto_upload_setting))) {
            prefsEditor.putBoolean(getString(R.string.auto_upload_setting), !autoUploadOption).commit();
        }
    }
}