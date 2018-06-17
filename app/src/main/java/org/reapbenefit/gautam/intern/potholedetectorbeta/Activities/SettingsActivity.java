package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.SettingsFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;


public class SettingsActivity extends AppCompatActivity {

    private FrameLayout frameLayout;
    private SwitchPreferenceCompat deleteFileOption;
    private SwitchPreferenceCompat autoUploadOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        frameLayout = (FrameLayout) findViewById(R.id.settings_frame);


        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_frame, new SettingsFragment())
                .commit();
    }
}


