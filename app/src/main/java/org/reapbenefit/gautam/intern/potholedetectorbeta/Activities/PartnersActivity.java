package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Switch;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

/**
 * Created by gautam on 15/08/17.
 */

public class PartnersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partners);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.menu_partners);
        Switch s = (Switch) toolbar.findViewById(R.id.stopSwitch);
        s.setVisibility(View.GONE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LocationManager lm;

    }

    public void openRBApp(View v){
        Uri webpage = Uri.parse("http://reapbenefit.org");
        Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
        startActivity(webIntent);
    }
}