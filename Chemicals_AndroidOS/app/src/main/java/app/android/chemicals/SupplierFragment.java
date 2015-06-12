package app.android.chemicals;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Sources:
// https://github.com/udacity/Sunshine-Version-2/tree/2.09_display_data
// http://developer.android.com/guide/components/fragments.html
// https://class.coursera.org/androidpart1-003/lecture
// http://developer.android.com/training/basics/data-storage/files.html

public class SupplierFragment extends Fragment implements View.OnClickListener {

    private static final String LOM = "LOM";
    private final String filename = "supplierData";
    private ArrayAdapter<String> mSupplierAdapter;
    private List<String> suppliersList;
    private Button refresh_button;
    private Button add_button;
    private boolean mAlreadyLoaded = false;
    private String URL = "http://cs496-final-proj.appspot.com/supplier";
    private String METHOD = "GET";
    private final static String TAG_EDIT_SUPPL = "TAG_EDIT_SUPPL";
    private String TOKEN;
    private String USER_ID = "";
    private String[] data_for_fragment;

    public SupplierFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // let fragment to handle menu events.
        setHasOptionsMenu(true);

        // get passed Token
        Bundle bundle = this.getArguments();
        String strings[]= bundle.getStringArray("DATA_ARRAY");
        USER_ID = strings[0];
        TOKEN = strings[1];
        Log.e(LOM, "TOKEN from SupplierActivity :" + TOKEN + "\nUSERID: " + USER_ID);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.supplier_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            fetchData(URL, METHOD);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // Checks if there is a connectivity
    public boolean isConnected(){
        boolean isConnected = false;
        Context context = getActivity();
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
        Context context = getActivity();

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
        Context ctx = getActivity();//.getApplicationContext();
        ArrayList<String> output = new ArrayList<String>();
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
                if (++counter == 3)
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


    // fetch Supppliers data either from internet,
    // or use from saved file if there is no connection
    public void fetchData(String url, String method){
        if(isConnected()){
            FetchSupplierTask supplierTask = new FetchSupplierTask();
            supplierTask.execute(url, method);
        }
        else{
            Toast.makeText(this.getActivity(),
                "No internet connection!\n" +
                    "Previously fetched data is shown", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Create empty data array for the ListView.
        //String[] data = {};
        //List<String> suppliersList = new ArrayList<String>(Arrays.asList(data));

        suppliersList = readFromFile();

        // Create an ArrayAdapter.
        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mSupplierAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_supplier, // The name of the layout ID.
                        R.id.list_supplier_textview, // The ID of the textview to populate.
                        suppliersList);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_supplier);
        listView.setAdapter(mSupplierAdapter);
        registerForContextMenu(listView);

        refresh_button = (Button) rootView.findViewById(R.id.refresh_button);
        refresh_button.setOnClickListener(this);

        add_button = (Button) rootView.findViewById(R.id.addSupp_button);
        add_button.setOnClickListener(this);

        // show updated list of Suppliers on loading
        fetchData(URL, METHOD);
        return rootView;

    }//onCreateView()


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.listview_supplier) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            //menu.setHeaderTitle(Countries[info.position]);
            menu.setHeaderTitle(suppliersList.get(info.position));
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
        String listItem = suppliersList.get(info.position);

        switch (menuItemName)
        {
            case "Edit":
                //Log.d(LOM, "Add Supplier button clicked");
                //Toast.makeText(this.getActivity(),
                //        String.format("menuItemName: %s\n listItemName: %s", menuItemName, listItem),
                //        Toast.LENGTH_LONG).show();

                EditSupplierFragment nextFrag = new EditSupplierFragment();

                // pass data to fragment
                data_for_fragment = new String[3];
                data_for_fragment[0] = listItem;
                data_for_fragment[1] = TOKEN;
                data_for_fragment[2] = USER_ID;

                Bundle bundle = new Bundle();
                bundle.putStringArray("DATA_ARRAY", data_for_fragment);
                //String abcd[]=bundle.getStringArray("DATA_ARRAY");
                nextFrag.setArguments(bundle);

                this.getChildFragmentManager().beginTransaction()
                        .replace(R.id.add_suplier, nextFrag, TAG_EDIT_SUPPL)
                        .addToBackStack(null)
                        .commit();
                fetchData(URL, METHOD);
                break;

            case "Delete":
                //Log.d(LOM, "Delete button clicked");
                String[] tokens = listItem.split("\n");
                //Log.d(LOM, "\n\nSupplier_data :" + listItem);
                String key_string = tokens[2];
                String url_delete = URL + "/" + key_string;
                fetchData(url_delete, "DELETE");

                Toast toast = Toast.makeText(getActivity(), tokens[0] + " is DELETED!", Toast.LENGTH_LONG);
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

    // https://github.com/codepath/android_guides/wiki/ViewPager-with-FragmentPagerAdapter
    private final static String TAG_ADD_SUPPL = "TAG_ADD_SUPPL";

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.addSupp_button:
                //Log.d(LOM, "Add Supplier button clicked");
                AddSupplierFragment nextFrag = new AddSupplierFragment();

                // pass data to fragment
                data_for_fragment = new String[2];
                //data_for_fragment[0] = listItem;
                data_for_fragment[0] = TOKEN;
                data_for_fragment[1] = USER_ID;

                Bundle bundle = new Bundle();
                bundle.putStringArray("DATA_ARRAY", data_for_fragment);
                //String abcd[]=bundle.getStringArray("DATA_ARRAY");
                nextFrag.setArguments(bundle);

                this.getChildFragmentManager().beginTransaction()
                        .replace(R.id.add_suplier, nextFrag, TAG_ADD_SUPPL)
                        .addToBackStack(null)
                        .commit();
                break;

            case R.id.refresh_button:
                //Log.d(LOM, "Refresh button clicked");
                fetchData(URL, METHOD);
                break;
        }//switch

    }// onClick()


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
        if(requestCode == "app.android.chemicals.Data" && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                String value = intent.getStringExtra(FRAGMENT_KEY);
                if(value != null) {
                    Log.v(TAG, "Data passed from Child fragment = " + value);
                }
            }
        }*/
        fetchData(URL, METHOD);
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
            final String SUPPLIERS = "suppliers";
            final String NAME = "name";
            final String WEBSITE = "website";
            final String KEY = "key";

            // Raw JSON String to JSON object
            JSONObject suppliersJson = new JSONObject(suppliersJsonStr);
            // save in the JSONArray list of suppliers
            JSONArray suppliersArray = suppliersJson.getJSONArray(SUPPLIERS);

            // String array to save each supplier
            String[] resultStrs = new String[suppliersArray.length()];
            for(int i = 0; i < suppliersArray.length(); i++) {
                // Strings to save data
                String name;
                String website;
                String key;

                // Get the JSON object representing the supplier
                JSONObject oneSupplier = suppliersArray.getJSONObject(i);
                name = oneSupplier.getString(NAME);
                website = oneSupplier.getString(WEBSITE);
                key = oneSupplier.getString(KEY);

                resultStrs[i] = name + "\n" + website + "\n" + key;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Supplier entry: " + s);
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
                URL url = new URL(url_string);

                Log.v(LOG_TAG, "Built URI " + url.toString());

                String credentials = USER_ID + ":" + TOKEN;
                Log.v(LOG_TAG, "!!!Built credentials!!!" + credentials);
                String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP); //or .DEFAULT

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
            Context ctx = getActivity();//.getApplicationContext();
            // create output stream object
            FileOutputStream outputStream;

            try {
                outputStream = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
                for (int i = 0; i < result.length; i++) {
                    result[i] = result[i] + "\n";
                    Log.d(LOM, "Input Item: " + result[i] + "<--EndItem\n");
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
                mSupplierAdapter.clear();
                writeToFile(result);
                for(String supplier : result) {
                    mSupplierAdapter.add(supplier);
                }//for

            }//if
        }//onPostExecute()
    }//FetchSupplierTask class
}//SupplierFragment

