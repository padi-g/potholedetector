package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;


public class SettingsActivity extends AppCompatActivity {

    SharedPreferences prefs;

    //TODO : Create a file with all the constants like names for prefs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("uploads",MODE_PRIVATE);

        CheckBox deleteChkBox = (CheckBox) findViewById(R.id.delete_checkbox);
        deleteChkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    prefs.edit().putBoolean("file_delete", true).commit();
                    Log.d("Preferences", "Inside isChecked Found file_delete = " + prefs.getBoolean("file_delete", false));
                    // set pref to delete
                }else {
                    prefs.edit().putBoolean("file_delete", false).commit();
                    Log.d("Preferences", "Inside isNotChecked Found file_delete = " + prefs.getBoolean("file_delete", false));
                    // set pref to no_delete
                }
            }
        });

        if(prefs.contains("file_delete")){
            Log.d("Preferences", "file_delete is found on prefs");
            if(prefs.getBoolean("file_delete", false)){
                Log.d("Preferences", "prefs are true");
                deleteChkBox.setChecked(true);

            }else {
                Log.d("Preferences", "prefs are false");
                deleteChkBox.setChecked(false);
            }
        }

        CheckBox autoUploadChkBox = (CheckBox) findViewById(R.id.auto_upload_checkbox);
        autoUploadChkBox.setChecked(true);
        autoUploadChkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    prefs.edit().putBoolean("auto_upload", true).commit();
                    Log.d("Preferences", "Inside isChecked Found auto_upload = " + prefs.getBoolean("auto_upload", true));
                    // set pref to delete
                }else {
                    prefs.edit().putBoolean("auto_upload", false).commit();
                    Log.d("Preferences", "Inside isNotChecked Found auto_upload = " + prefs.getBoolean("auto_upload", false));
                    // set pref to no_delete
                }
            }
        });

        if(prefs.contains("auto_upload")){
            Log.d("Preferences", "auto_upload is found on prefs");
            if(prefs.getBoolean("auto_upload", true)){
                Log.d("Preferences", "prefs are true");
                autoUploadChkBox.setChecked(true);
            }else {
                Log.d("Preferences", "prefs are false");
                autoUploadChkBox.setChecked(false);
            }
        }else {
            Log.d("Preferences", "auto_upload is not found on prefs");
            prefs.edit().putBoolean("auto_upload", true).commit();
        }
    }

    // TODO: add analytics events in all new activities

}
