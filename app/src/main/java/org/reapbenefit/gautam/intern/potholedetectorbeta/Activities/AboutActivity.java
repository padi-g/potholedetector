package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class AboutActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private TextView privacyPolicy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.menu_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        privacyPolicy = (TextView) findViewById(R.id.privacy_policy);

        privacyPolicy.setClickable(true);
        privacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/1lm70g18W2nznMtY2uVkMnLdXidFqNG3F7Azl8bEDJGo/edit?usp=sharing"));
                startActivity(browserIntent);
            }
        });

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logAnalytics("about_opened");

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

    public void logAnalytics(String data){
        Bundle b = new Bundle();
        b.putString("AboutActivity", data);
        mFirebaseAnalytics.logEvent(data, b);
    }
}
