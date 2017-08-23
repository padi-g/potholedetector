package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Switch;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class CreditsActivity extends AppCompatActivity {

    Switch StartsStop;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.menu_about);
        Switch s = (Switch) toolbar.findViewById(R.id.stopSwitch);
        s.setVisibility(View.GONE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void sendEmail(View v){
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, "gp712@snu.edu.in");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Road Quality Audit android app");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
