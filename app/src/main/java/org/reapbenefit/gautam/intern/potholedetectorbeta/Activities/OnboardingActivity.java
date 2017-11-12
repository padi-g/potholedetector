package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

/**
 * Created by gautam on 12/11/17.
 */

public class OnboardingActivity extends AppCompatActivity {

    private static final String COMPLETED_ONBOARDING_PREF_NAME = "onboard_complete";
    private FirebaseAuth mAuth;
    private SignInButton SignIn;
    private static final int RC_SIGN_IN = 531;
    private GoogleApiClient mGoogleApiClient;
    private TextView mUser_nameTextView;
    private Boolean signedIn;
    private ProgressDialog dialog;
    private Button continueButton;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        // Check if we need to display our OnboardingFragment
        if (!sharedPreferences.getBoolean(
                OnboardingActivity.COMPLETED_ONBOARDING_PREF_NAME, false)) {
            // The user hasn't seen the OnboardingFragment yet, so show it
            setContentView(R.layout.activity_onboarding);
            mAuth = FirebaseAuth.getInstance();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Log.i("Sign in", "Failed");
                        }
                    } /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            mUser_nameTextView = (TextView) findViewById(R.id.name);
            SignIn = (SignInButton) findViewById(R.id.start_sign_in);
            continueButton = (Button) findViewById(R.id.confirm_button);

            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    continueToMainActivity();
                }
            });

            if(mAuth.getCurrentUser() != null) {
                Log.d("Onboarding", "Using google auth");
                sharedPreferences.edit().putBoolean(COMPLETED_ONBOARDING_PREF_NAME, true).commit();
                signedIn = true;
                continueToMainActivity();
            }
            else {
                signedIn = false;
            }


            SignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    signIn();

                }
            });


        }else {
            Log.d("Onboarding", "Using shared preferences");
            continueToMainActivity();
        }
    }

    public void signIn() {
        SignIn.setEnabled(false);
        Log.i("Sign in", "Trying");
        signedIn = true;
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            handleSignInResult(result);

            GoogleSignInAccount account = result.getSignInAccount();
            firebaseAuthWithGoogle(account);

        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("Sign in", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mUser_nameTextView.setVisibility(View.VISIBLE);
            mUser_nameTextView.setText(acct.getDisplayName());
            sharedPreferences.edit().putBoolean(COMPLETED_ONBOARDING_PREF_NAME, true).commit();
            continueButton.setVisibility(View.VISIBLE);

            dialog = ProgressDialog.show(this, "", "Signing in...",
                    true);
            dialog.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    dialog.dismiss();
                }
            }, 3000); // 3000 milliseconds delay

        } else {
            // Signed out, show unauthenticated UI.
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        if(acct != null) {
            Log.d("firebase auth", "firebaseAuthWithGoogle:" + acct.getId());

            AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("firebase auth", "signInWithCredential:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                //update UI
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("firebase auth", "signInWithCredential:failure", task.getException());
                                Toast.makeText(OnboardingActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                //update UI
                            }

                            // ...
                        }
                    });
        }else {
        }
    }

    private void continueToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
