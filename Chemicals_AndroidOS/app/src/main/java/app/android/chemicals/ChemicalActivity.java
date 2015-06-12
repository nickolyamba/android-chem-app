package app.android.chemicals;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Source: //http://stackoverflow.com/questions/24819652
public class ChemicalActivity extends AppCompatActivity implements View.OnClickListener{

    private final String LOM = "LOM";
    private final String filename = "chemData";
    private ArrayAdapter<String> mChemAdapter;
    private List<String> chemList;
    private Button refresh_button;
    private Button add_button;
    private boolean mAlreadyLoaded = false;
    private final String URL = "http://cs496-final-proj.appspot.com/chemical";
    private final String METHOD = "GET";
    private String TOKEN="";
    private String USER_ID = "";
    private String[] data_for_fragment;
    private final static String TAG_ADD_CHEM = "TAG_ADD_CHEM";
    private final static String TAG_EDIT_CHEM = "TAG_EDIT_CHEM";
    private final int CHEM_ROWS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chemical_main);

        Intent intent = getIntent();
        TOKEN = intent.getStringExtra("Token");
        USER_ID = intent.getStringExtra("USER_ID");
        Toast.makeText(this, "TOKEN: " + TOKEN +"\n"+"USER: "+USER_ID, Toast.LENGTH_LONG).show();

        //suppliersList = readFromFile();
        chemList = new ArrayList<>(Arrays.asList("xyz", "abc", "blah"));

        chemList = readFromFile();

        // Create an ArrayAdapter.
        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mChemAdapter = new ArrayAdapter<String>(
                        this, // The current context (this activity)
                        R.layout.listitem, // The name of the layout ID.
                        R.id.list_item, // The ID of the textview to populate.
                        chemList);

        //View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) findViewById(R.id.listview_chemical);
        listView.setAdapter(mChemAdapter);
        registerForContextMenu(listView);

        refresh_button = (Button) findViewById(R.id.chem_refresh_button);
        refresh_button.setOnClickListener(this);

        add_button = (Button) findViewById(R.id.addChem_button);
        add_button.setOnClickListener(this);

        // show updated list of Suppliers on loading
        fetchData(URL, METHOD);
        //return rootView;

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.listview_chemical) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(chemList.get(info.position));
            String[] menuItems = getResources().getStringArray(R.array.menu);
            for(int i = 0; i < menuItems.length; i++){
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = getResources().getStringArray(R.array.menu);
        String menuItemName = menuItems[menuItemIndex];
        String listItem = chemList.get(info.position);

        switch (menuItemName)
        {
            case "Edit":
                Log.d(LOM, String.format("menuItemName: %s listItem: %s", menuItemName, listItem));
               // Toast.makeText(this,
               //        String.format("menuItemName: %s\n listItemName: %s", menuItemName, listItem),
               //       Toast.LENGTH_LONG).show();

                EditChemicalFragment nextFrag = new EditChemicalFragment();

                // pass data to fragment
                data_for_fragment = new String[3];
                data_for_fragment[0] = listItem;
                data_for_fragment[1] = TOKEN;
                data_for_fragment[2] = USER_ID;

                Bundle bundle = new Bundle();
                bundle.putStringArray("DATA_ARRAY", data_for_fragment);
                //String abcd[]=bundle.getStringArray("DATA_ARRAY");
                nextFrag.setArguments(bundle);

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.edit_chem_container, nextFrag, TAG_EDIT_CHEM)
                        .addToBackStack(null)
                        .commit();
                //fetchData(URL, METHOD);
                break;

            case "Delete":
                //Log.d(LOM, "Delete button clicked");
                String[] tokens = listItem.split("\n");
                //Log.d(LOM, "\n\nChemData in DELETE:" + listItem);
                String key_string = tokens[4];
                String url_delete = URL + "/" + key_string;
                fetchData(url_delete, "DELETE");

                Toast toast = Toast.makeText(this, tokens[0] + " is DELETED!", Toast.LENGTH_LONG);
                View view = toast.getView();
                view.setBackgroundResource(R.drawable.red_toast);
                toast.show();

                fetchData(URL, METHOD);
                break;
            default:
                return super.onContextItemSelected(item);
        }//switch

        return true;
    }//onContextItemSelected()


    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.addChem_button:
                Log.d(LOM, "Add Supplier button clicked");
                AddChemicalFragment newFrag = new AddChemicalFragment();

                // pass data to fragment
                data_for_fragment = new String[2];
                //data_for_fragment[0] = listItem;
                data_for_fragment[0] = TOKEN;
                data_for_fragment[1] = USER_ID;

                Bundle bundle = new Bundle();
                bundle.putStringArray("DATA_ARRAY", data_for_fragment);
                //String abcd[]=bundle.getStringArray("DATA_ARRAY");
                newFrag.setArguments(bundle);

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.add_chem_container, newFrag, TAG_EDIT_CHEM)
                        .addToBackStack(null)
                        .commit();

                fetchData(URL, METHOD);
                break;

            case R.id.chem_refresh_button:
                Log.d(LOM, "Refresh button clicked");
                fetchData(URL, METHOD);
                break;
        }//switch

    }// onClick()



    // Checks if there is a connectivity
    public boolean isConnected(){
        boolean isConnected = false;
        Context context = getApplicationContext();
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    // http://stackoverflow.com/questions/8867334/check-if-a-file-exists-before-calling-openfileinput
    //check if file exist before opening it up
    public boolean isFileExist(){
        Context context = getApplicationContext();

        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    // Read from saved file in internal storage if there is no
    // internet connectivity.
    // returns the list of suppliers in ArrayList<String>
    public  ArrayList<String> readFromFile(){
        // activity extends Context anyway,so no need to get it
        Context ctx = getApplicationContext();
        ArrayList<String> output = new ArrayList<String>();;
        FileInputStream inputStream = null;
        BufferedReader reader = null;
        int counter = 0;

        if(!isFileExist())
        {
            output.add("No Data in phone storage yet...\n");
            return output;
        }


        try
        {
            inputStream = ctx.openFileInput(filename);
            StringBuffer buffer = new StringBuffer();

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // add line to make debugging easier
                buffer.append(line).append("\n");
                // after 3 lines are read (name, website, key),
                // add to entity to the list
                if (++counter == CHEM_ROWS)
                {
                    output.add(buffer.toString());
                    //Log.d(LOM, "List Item: " + buffer.toString() + "<--EndItem\n");
                    buffer.delete(0, buffer.length());
                    counter = 0;
                }//if
            }//while

            //Log.d(LOM, "File is read: ", output.);

            //reader.close();
            //inputStream.close();
        }
        catch (FileNotFoundException ex) {
            Logger.getLogger(SupplierFragment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SupplierFragment.class.getName()).log(Level.SEVERE, null, ex);

        } finally {
            try {
                reader.close();
                inputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(SupplierFragment.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return output;
    }//readFromFile()


    // fetch  data either from internet,
    // or use from saved file if there is no connection
    public void fetchData(String url, String method){
        if(isConnected()){
            FetchSupplierTask supplierTask = new FetchSupplierTask();
            supplierTask.execute(url, method);
        }
        else{
            Toast.makeText(this,
                    "No internet connection!\n" +
                            "Previously fetched data is shown", Toast.LENGTH_LONG).show();
        }
    }


    /**
     *  FetchSupplierTask extends AsyncTask()
     *
     *  Creates network connection to internet using parallel thread
     *  separate from main UI thread. doInBackground() function creates thread
     *  where does all the job.
     *
     *  onPostExecute() runs in main UI thread, receives data from doInBackground()
     *  through parameter
     *
     *  http://developer.android.com/reference/android/os/AsyncTask.html
     */
    public class FetchSupplierTask extends AsyncTask<Object, Void, String[]> {
        //public class FetchSupplierTask extends AsyncTask<Void, Void, String[]> {
        // get the name of the name of class
        private final String LOG_TAG = FetchSupplierTask.class.getSimpleName();

        /**
         * getSupplierDataFromJson()
         *  Parses JSON to get name, website and key
         *  parameters: JSON string
         *  return: String of suppliers
         */
        private String[] getSupplierDataFromJson(String suppliersJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String CHEMICALS = "chemicals";
            final String NAME = "name";
            final String SUPPL_KEY = "supplier";
            final String CATALOG = "catalog";
            final String CAS = "cas";
            final String KEY = "key";

            // Raw JSON String to JSON object
            JSONObject chemJson = new JSONObject(suppliersJsonStr);
            // save in the JSONArray list of suppliers
            JSONArray chemArray = chemJson.getJSONArray(CHEMICALS);

            // String array to save each supplier
            String[] resultStrs = new String[chemArray.length()];
            for(int i = 0; i < chemArray.length(); i++) {
                // Strings to save data
                String name;
                String catalog;
                String cas;
                String supp_key;
                String key;

                // Get the JSON object representing the supplier
                JSONObject chemItem = chemArray.getJSONObject(i);
                name = chemItem.getString(NAME);
                catalog = chemItem.getString(CATALOG);
                cas = chemItem.getString(CAS);
                key = chemItem.getString(KEY);
                supp_key = chemItem.getString(SUPPL_KEY);

                resultStrs[i] = name + "\n" + catalog + "\n" + cas + "\n" + supp_key + "\n" + key;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Chemical entry: " + s);
            }
            return resultStrs;

        }


        @Override
        protected String[] doInBackground(Object... params) {

            String url_string = (String) params[0];
            //String name_value = (String) params[1];
            String method = (String) params[1];

            // create HttpURLConnection object for connection.
            HttpURLConnection urlConnection = null;
            // create Buffer object to save the data stream.
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String suppliersJsonStr = null;

            try {
                //URL url = new URL("http://cs496-final-proj.appspot.com/supplier");
                java.net.URL url = new URL(url_string);

                Log.v(LOG_TAG, "Built URI " + url.toString());

                String credentials = USER_ID + ":" + TOKEN;
                Log.v(LOG_TAG, "!!!Built credentials!!!\n" + credentials);
                String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(method);
                urlConnection.setRequestProperty("Authorization", "basic " + base64EncodedCredentials);
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }

                if(method == "DELETE")
                {
                    return null;

                }//if()

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // add line to make debugging easier
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                suppliersJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Supplier string: " + suppliersJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            // parse JSON string
            try {
                return getSupplierDataFromJson(suppliersJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // if there was an error getting or parsing json, return null.
            return null;
        }

        // http://developer.android.com/training/basics/data-storage/files.html
        public void writeToFile(String [] result){
            // activity extends Context anyway,so no need to get it
            Context ctx = getApplicationContext();//.getApplicationContext();
            // create output stream object
            FileOutputStream outputStream;

            try {
                outputStream = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
                for (int i = 0; i < result.length; i++) {
                    result[i] = result[i] + "\n";
                    Log.d(LOM, "Input writeToFile() Item: " + result[i] + "<--EndItem\n");
                    outputStream.write(result[i].getBytes());
                }
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//writeToFile()


        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mChemAdapter.clear();
                writeToFile(result);
                for(String supplier : result) {
                    mChemAdapter.add(supplier);
                }//for

            }//if
        }//onPostExecute()
    }//FetchSupplierTask class






}//ChemicalActivity











/*
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
*/





/*
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
*/
