package app.android.chemicals;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

// Source: //http://stackoverflow.com/questions/24819652/backstack-is-not-working-with-nested-fragments

public class SupplierActivity extends AppCompatActivity implements View.OnClickListener{

    private String TOKEN="";
    private String USER_ID = "";
    private String[] data_for_fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.supp_act_main);

        Intent intent = getIntent();
        TOKEN = intent.getStringExtra("Token");
        USER_ID = intent.getStringExtra("USER_ID");
        Log.e(LOM, "TOKEN: " + TOKEN + "\n" + "USER: " + USER_ID);

        // pass data to fragment
        data_for_fragment = new String[2];
        data_for_fragment[0] = USER_ID;
        data_for_fragment[1] = TOKEN;

        SupplierFragment nextFrag = new SupplierFragment();

        Bundle bundle = new Bundle();
        bundle.putStringArray("DATA_ARRAY", data_for_fragment);
        nextFrag.setArguments(bundle);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, nextFrag)
                    .commit();
        }


    }

    private static final String LOM = "LOM";
    public void onClick(View v) {

    }//onClick()


    //http://stackoverflow.com/questions/24819652/backstack-is-not-working-with-nested-fragments
    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment frag : fm.getFragments()) {
            if (frag.isVisible()) {
                FragmentManager childFm = frag.getChildFragmentManager();
                if (childFm.getBackStackEntryCount() > 0) {
                    for (Fragment childfragnested: childFm.getFragments()) {
                        FragmentManager childFmNestManager = childfragnested.getFragmentManager();
                        if(childfragnested.isVisible()) {
                            childFmNestManager.popBackStack();
                            return;
                        }
                    }
                }
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}

