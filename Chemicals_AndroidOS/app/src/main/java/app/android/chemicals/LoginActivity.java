package app.android.chemicals;

import android.accounts.Account;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LoginActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static  final String SERVER_CLIENT_ID = "284185944754-mjgn7fohvqblb8813s1gmq1q3g8jghua.apps.googleusercontent.com";

    final private String CLIENT_ID = "284185944754-9vlg5qrqscvkvngli4rp01sn4c9a38q2.apps.googleusercontent.com";
    final private List<String> SCOPES = Arrays.asList(new String[]{
            "https://www.googleapis.com/auth/plus.login"
    });



    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    private String USER_ID = "";
    private String ID_TOKEN = "";

    /**
     * True if the sign-in button was clicked.  When true, we know to resolve all
     * issues preventing sign-in without waiting.
     */
    private boolean mSignInClicked;

    /**
     * True if we are in the process of resolving a ConnectionResult
     */
    private boolean mIntentInProgress;

    /* View to display current status (signed-in, signed-out, disconnected, etc) */
    private TextView mStatus;

    private static final String TAG = "LoginActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope("profile"))
                .build();

        // Set up button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // Set up view instances
        mStatus = (TextView) findViewById(R.id.status);
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        //mGoogleApiClient.disconnect();
        //mGoogleApiClient.connect();
        //findViewById(R.id.disconnect_button).setOnClickListener(this);
        // Set up button click listeners
        //findViewById(R.id.sign_in_button).setOnClickListener(this);
        //findViewById(R.id.sign_out_button).setOnClickListener(this);
        //findViewById(R.id.disconnect_button).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //mGoogleApiClient.disconnect();
        mGoogleApiClient.connect();
        //findViewById(R.id.disconnect_button).setOnClickListener(this);
        // Set up button click listeners
        //findViewById(R.id.sign_in_button).setOnClickListener(this);
        //findViewById(R.id.sign_out_button).setOnClickListener(this);
        //findViewById(R.id.disconnect_button).setOnClickListener(this);
/*
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress) {
            if (mSignInClicked && result.hasResolution()) {
                // The user has already clicked 'sign-in' so we attempt to resolve all
                // errors until the user is signed in, or they cancel.
                try {
                    result.startResolutionForResult(this, RC_SIGN_IN);
                    mIntentInProgress = true;

                } catch (IntentSender.SendIntentException e) {
                    // The intent was canceled before it was sent.  Return to the default
                    // state and attempt to connect to get an updated ConnectionResult.
                    mIntentInProgress = false;
                    mGoogleApiClient.connect();

                }
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mSignInClicked = false;
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
        new GetIdTokenTask().execute("");

    }

    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + responseCode + ":" + intent);
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;

            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.reconnect();
            }


        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

/*
    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.disconnect();
    }*/


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                // User clicked the sign-in button, so begin the sign-in process and automatically
                // attempt to resolve any errors that occur.
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }
                mStatus.setText(R.string.signing_in);
                // [START sign_in_clicked]
                mSignInClicked = true;
                mGoogleApiClient.connect();

                // [END sign_in_clicked]
                break;
            case R.id.sign_out_button:
                // Clear the default account so that GoogleApiClient will not automatically
                // connect in the future.
                // [START sign_out_clicked]
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }
                // [END sign_out_clicked]
                //updateUI(false);
                break;
            case R.id.disconnect_button:
                // Revoke all granted permissions and clear the default account.  The user will have
                // to pass the consent screen to sign in again.
                // [START disconnect_clicked]
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                    //android.os.Process.killProcess(android.os.Process.myPid());
                    //System.exit(1);
                }
                // [END disconnect_clicked]
                //updateUI(false);
                break;
        }
    }//onClick


    private class GetIdTokenTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
            Account account = new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            String scopes = "audience:server:client_id:" + SERVER_CLIENT_ID;//+":api_scope:"+"profile"; // Not the app's client ID.
            scopes = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
            String idToken = "";
            try {
                idToken = GoogleAuthUtil.getToken(getApplicationContext(), account, scopes);
            } catch (IOException e) {
                Log.e(TAG, "Error retrieving ID token.", e);
            } catch (GoogleAuthException e) {
                Log.e(TAG, "Error retrieving ID token.", e);
            }
            return idToken;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "ID token: " + result);
            //mStatus.setText(result);
            //Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            new GetAuthedUserName().execute(result);

        }//onPostExecute()

    }//GetIdTokenTask()


    private class GetAuthedUserName extends AsyncTask<String, Void, String> {
        private String getToken(String responseBody) throws JSONException{
            String result;
            final String TOKEN = "user_id";
            JSONObject resultJson = new JSONObject(responseBody);

            result = resultJson.getString(TOKEN);
            Log.e(TAG, "USER_ID: " + result);
            return result;
        }
        @Override
        protected String doInBackground(String... params) {
            String USERID = "";
            String idToken = params[0];
            ID_TOKEN = idToken;
            String responseBody = "";
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://cs496-final-proj.appspot.com/signin");

            try {
                List nameValuePairs = new ArrayList(1);
                nameValuePairs.add(new BasicNameValuePair("idToken", idToken));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                responseBody = EntityUtils.toString(response.getEntity());
                Log.e(TAG, "Returned From Backend: " + responseBody);
            } catch (ClientProtocolException e) {
                Log.e(TAG, "Error sending ID token to backend.", e);
            } catch (IOException e) {
                Log.e(TAG, "Error sending ID token to backend.", e);
            }

            try {
                USERID = getToken(responseBody);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return USERID;
        }//doInBackground()

        @Override
        protected void onPostExecute(String result)  {
            //Log.d(TAG, "Returned from BackEnd: " + result);
            //Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            USER_ID = result;

            if(USER_ID != "" && ID_TOKEN!=""){
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                mainIntent.putExtra("USER_ID", USER_ID);
                mainIntent.putExtra("TOKEN", ID_TOKEN);
                Log.e(TAG, "USER_ID: " + USER_ID);//got null
                startActivity(mainIntent);
            }
        }//onPostExecute()

    }//GetAuthedUserName()

}//LoginActivity()

