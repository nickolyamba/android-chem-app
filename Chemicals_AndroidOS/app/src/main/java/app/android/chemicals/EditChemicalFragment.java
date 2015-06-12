package app.android.chemicals;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EditChemicalFragment extends Fragment implements View.OnClickListener {

    private static final String LOM = "LOM";
    private Button add_button;
    private EditText name;
    private EditText catalog;
    private EditText cas;

    private String name_string;
    private String catalog_string;
    private String cas_string;
    private String key_string;
    private String chem_data;
    private String TOKEN;
    private String USER_ID = "";

    private ArrayAdapter<Supplier> mSupplierAdapter;
    private List<Supplier> suppliersList;
    private String supplierID = "";
    private Spinner suppSpinner;

    private String URL = "http://cs496-final-proj.appspot.com/supplier";
    private String METHOD = "GET";

    public EditChemicalFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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


    public static boolean hasText(EditText editText) {
        final String REQUIRED_MSG = "Text Required";
        String text = editText.getText().toString().trim();
        editText.setError(null);

        // length 0 means there is no text
        if (text.length() == 0) {
            editText.setError(REQUIRED_MSG);
            return false;
        }

        return true;
    }


    // fetch Supppliers data either from internet,
    // or use from saved file if there is no connection
    public void fetchData(String url, String method){
        if(isConnected()){
            FetchSupplierData supplierTask = new FetchSupplierData();
            supplierTask.execute(url, method, USER_ID, TOKEN);
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

        View view = inflater.inflate(R.layout.edit_chemical, container, false);

        // get passed Token and Chem Data from Activity
        Bundle bundle = this.getArguments();
        String strings[]= bundle.getStringArray("DATA_ARRAY");
        chem_data = strings[0];
        TOKEN = strings[1];
        USER_ID = strings[2];
        Log.e(LOM, "chem_data from ChemActivity in EditCHemFrag: " + chem_data + "\n" + TOKEN + "\n" + USER_ID);

        //http://stackoverflow.com/questions/22350683/
        String[] tokens = chem_data.split("\n");

        Log.d(LOM, "\n\nSupplier_data :" + chem_data);
        name_string = tokens[0];
        catalog_string = tokens[1];
        cas_string = tokens[2];
        key_string = tokens[4];

        // set EditText values and button
        name = (EditText) view.findViewById(R.id.chem_name_edit);
        name.setText(name_string, TextView.BufferType.EDITABLE);
        catalog = (EditText) view.findViewById(R.id.chem_catalog_edit);
        catalog.setText(catalog_string, TextView.BufferType.EDITABLE);
        cas = (EditText) view.findViewById(R.id.chem_cas_edit);
        cas.setText(cas_string, TextView.BufferType.EDITABLE);
        add_button = (Button) view.findViewById(R.id.submit_supp_btn);
        add_button.setOnClickListener(this);

        //Fill The Spinner
        // put dummy Object in suppliersList
        Supplier supp1 = new Supplier();
        supp1.name = "SELECT SUPPLIER";
        supp1.website = "www.Supp#1";
        supp1.id = "SuppID";

        suppliersList = new ArrayList<>(Arrays.asList(supp1));
        mSupplierAdapter = new ArrayAdapter(getActivity(), R.layout.spinner, suppliersList);
        suppSpinner = (Spinner) view.findViewById(R.id.chem_supp_edit);
        suppSpinner.setAdapter(mSupplierAdapter);
        suppSpinner.setSelection(0);

        fetchData(URL, METHOD);

        // http://stackoverflow.com/questions/23449270
        suppSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // Get the currently selected State object from the spinner
                Supplier supp = (Supplier) suppSpinner.getSelectedItem();
                supplierID = supp.id;
                Log.d(LOM, "SuppID on ItemSelected :" + supplierID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return view;
    }

    //"name=LiOH&cas=652-321-68&supplier=6192449487634432&catalog=20165-50G"
    public String createParams(String name, String supplierID, String catalog, String cas)
    {
        String params = "name=" + name + "&" +
                "supplier=" + supplierID + "&" +
                "catalog=" + catalog + "&" +
                "cas=" + cas;

        return params;
    }

    @Override
    public void onClick(View v) {
        if(isConnected()){
            // if required fields are filled
            if (hasText(name)&& hasText(catalog)){
                final String NAME = name.getText().toString();
                final String CATALOG = catalog.getText().toString();
                final String CAS = cas.getText().toString();
                //https://cs496-final-proj.appspot.com/chemical
                final String url_put = "https://cs496-final-proj.appspot.com/chemical"+"/"+key_string;

                //Log.d(LOM, "Submit clicked :" + NAME + " " + WEBSITE);

                String params ="";

                params = createParams(NAME, supplierID, CATALOG, CAS);
                ConnectoinTask chemTask = new ConnectoinTask();
                chemTask.execute(url_put, params, "PUT");
                /** Option to automatically return on previous screen
                 FragmentManager manager = getActivity().getSupportFragmentManager();
                 FragmentTransaction trans = manager.beginTransaction();
                 trans.remove(this);
                 trans.commit();
                 manager.popBackStack();
                 */
            }//if
        }
        // no connection
        else
            Toast.makeText(this.getActivity(),
                    "No internet connection!\n" +
                            "Turn on connection and continue", Toast.LENGTH_LONG).show();

    }//onClick()


    private class FetchSupplierData extends FetchSuppliers{

        @Override
        protected void onPostExecute(Supplier[] result) {
            if (result != null) {
                //Log.d(LOM, "onPostExecute() 1st Supplier:" + result[0].name);
                mSupplierAdapter.clear();
                //writeToFile(result);
                //suppliersList = new ArrayList<Supplier>(Arrays.asList(result));
                for(Supplier supplier : result) {
                    mSupplierAdapter.add(supplier);
                }//for
                // set supplierID on the fisrt item in the adapter
                Supplier supp_one = mSupplierAdapter.getItem(0);
                supplierID = supp_one.id;
                //Log.d(LOM, "SuppID on onPostExecute:" + supplierID);

            }//if
        }//onPostExecute()
    }

    /**
     *  ConnectoinTask extends AsyncTask()
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
    public class ConnectoinTask extends AsyncTask<Object, Void, String> {
        // get the name of the name of class
        private final String LOG_TAG = ConnectoinTask.class.getSimpleName();

        /**
         * getSupplierDataFromJson()
         *  Parses JSON to get name, website and key
         *  parameters: JSON string
         *  return: String of suppliers
         */
        private String getSupplierDataFromJson(String suppliersJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            //final String CHEMICALS = "chemicals";
            final String NAME = "name";
            final String SUPPL_KEY = "supplier";
            final String CATALOG = "catalog";
            final String CAS = "cas";
            final String KEY = "key";

            // Raw JSON String to JSON object
            JSONObject chemItem = new JSONObject(suppliersJsonStr);
            // save in the JSONArray list of suppliers
            //JSONArray chemArray = chemJson.getJSONArray(CHEMICALS);

            // Strings to save data
            String name;
            String catalog;
            String cas;
            String supp_key;
            String key;
            String resultStr;

            // Get the JSON object representing the supplier

            name = chemItem.getString(NAME);
            catalog = chemItem.getString(CATALOG);
            cas = chemItem.getString(CAS);
            key = chemItem.getString(KEY);
            supp_key = chemItem.getString(SUPPL_KEY);

            resultStr = name;// + "\n" + catalog + "\n" + key +"\n"+ cas +
                   // "\n" + catalog + "\n" + supp_key;

            Log.v(LOG_TAG, "Chemical Edited: " + resultStr);
            return resultStr;

        }

        @Override
        protected String doInBackground(Object... params) {

            // If there's not enough params - return.
            if (params.length < 3) {
                return null;
            }

            String url_string = (String) params[0];
            String name_value = (String) params[1];
            String method = (String) params[2];


            // create HttpURLConnection object for connection.
            HttpURLConnection urlConnection = null;
            // create Buffer object to save the data stream.
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String suppliersJsonStr = null;

            try
            {
                URL url = new URL(url_string);

                Log.v(LOG_TAG, "Built URI " + url_string);

                String credentials = USER_ID + ":" + TOKEN;
                Log.v(LOG_TAG, "!!!Built credentials!!!\n" + credentials);
                String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                // Create the request to cloud and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(method);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Authorization", "basic " + base64EncodedCredentials);
                urlConnection.connect();

                // http://stackoverflow.com/questions/9767952/how-to-add-parameters-to-httpurlconnection-using-post
                // http://developer.android.com/reference/java/net/HttpURLConnection.html
                OutputStream outStream = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(outStream, "UTF-8"));
                writer.write(name_value);
                writer.flush();
                writer.close();
                outStream.close();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                // get new line
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                // Stream was empty.  No point in parsing.
                if (buffer.length() == 0) {

                    return null;
                }
                // save as a String
                suppliersJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Supplier string: " + suppliersJsonStr);
            }//try

            catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            }//catch

            finally {
                if (urlConnection != null)
                    urlConnection.disconnect();

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }//catch
                }//if
            }//finally

            // parse JSON and return string
            try {
                return getSupplierDataFromJson(suppliersJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }//catch

            return null;
        }//doInBackground

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                result = "Edited Chemical: "+result;
                name.setText("");
                catalog.setText("");
                cas.setText("");

                Toast toast = Toast.makeText(getActivity(), result, Toast.LENGTH_LONG);
                View view = toast.getView();
                view.setBackgroundResource(R.drawable.red_toast);
                toast.show();
            }
        }
    }//Async

}
