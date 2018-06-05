package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

/**
 * Created by gautam on 14/07/17.
 */

public class LoginActivity extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 530;
    private TextView mStatusTextView, mUser_nameTextView;

    private SignInButton SignIn;
    private Button SignOut;
    private Boolean signedIn;
    private FirebaseAuth mAuth;

    private ProgressDialog dialog;

    ApplicationClass app;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        app = ApplicationClass.getInstance();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.menu_login);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        mStatusTextView = (TextView) findViewById(R.id.login_details);
        mUser_nameTextView = (TextView) findViewById(R.id.users_name);
        SignIn = (SignInButton) findViewById(R.id.start_sign_in);
        SignOut = (Button) findViewById(R.id.start_sign_out);

        if(mAuth.getCurrentUser() != null) {
            signedIn = true;
            SignIn.setVisibility(View.GONE);
            if(app.isTripInProgress())
                SignOut.setVisibility(View.INVISIBLE);
            else    SignOut.setVisibility(View.VISIBLE);

        }
        else {
            signedIn = false;
            SignOut.setVisibility(View.GONE);
            SignIn.setVisibility(View.VISIBLE);
        }

        if(signedIn) {
            mUser_nameTextView.setText(mAuth.getCurrentUser().getDisplayName());
            mStatusTextView.setText("You have been signed in using \n" + mAuth.getCurrentUser().getEmail());
            mStatusTextView.setVisibility(View.VISIBLE);
        }else{
            mStatusTextView.setVisibility(View.GONE);
        }

        SignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    signIn();

            }
        });

        SignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });

    }

    public void signIn() {
        SignIn.setVisibility(View.GONE);
        SignOut.setVisibility(View.VISIBLE);
        //must prevent deletion of old user data
        SharedPreferences logoutPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        logoutPreferences.edit().putBoolean("loggedOut", false).commit();
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
            mStatusTextView.setVisibility(View.VISIBLE);
            mStatusTextView.setText("You have been signed in using " + acct.getEmail());
            mUser_nameTextView.setText(acct.getDisplayName());

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

    private void signOut() {

        SignOut.setVisibility(View.GONE);
        SignIn.setVisibility(View.VISIBLE);


        //must delete data of previous user from storage on signing out
        SharedPreferences logoutPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        logoutPreferences.edit().putBoolean("loggedOut", true).commit();

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.i("Sign out", "yes");
                        mStatusTextView.setVisibility(View.GONE);
                        mUser_nameTextView.setText("Please SIGN IN");

                    }
                });

        signedIn = false;
        FirebaseAuth.getInstance().signOut();

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
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                //update UI
                            }

                            // ...
                        }
                    });
        }else {
            signOut();
        }
    }


}
