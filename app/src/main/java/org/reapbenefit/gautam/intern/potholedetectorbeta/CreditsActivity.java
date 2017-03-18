package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Switch;

public class CreditsActivity extends AppCompatActivity {

    Switch StartsStop;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setTitle("Credits");

        StartsStop = (Switch) findViewById(R.id.stopSwitch);
        StartsStop.setVisibility(View.GONE);
    }
}
