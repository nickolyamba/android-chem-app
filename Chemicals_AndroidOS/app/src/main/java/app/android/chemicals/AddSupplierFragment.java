package app.android.chemicals;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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


public class AddSupplierFragment extends Fragment implements View.OnClickListener {

    private static final String LOM = "LOM";
    private Button add_button;
    private EditText name;
    private EditText website;
    private String TOKEN;
    private String USER_ID = "";
    TextView textToSet;

    public AddSupplierFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        //setHasOptionsMenu(false);
        // get passed Token
        Bundle bundle = this.getArguments();
        String strings[]= bundle.getStringArray("DATA_ARRAY");
        TOKEN = strings[0];
        USER_ID = strings[1];
        Log.e(LOM, "TOKEN in AddSupplierFragment :" + TOKEN + "\nUSERID: " + USER_ID);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.add_suplier, container, false);

        // create button
        add_button = (Button) view.findViewById(R.id.submit_supp_btn);
        add_button.setOnClickListener(this);

        // get EditText values
        name = (EditText) view.findViewById(R.id.supp_name);
        website = (EditText) view.findViewById(R.id.supp_website);

        // assign text to return to id, then set it in OnPostExecute()
        //textToSet = (TextView) view.findViewById(R.id.return_text);

        return view;
    }

    public String createParams(String name, String website)
    {
        String params = "name=" + name + "&" + "website=" + website;

        return params;
    }

    @Override
    public void onClick(View v) {
        if(isConnected()){
            if (hasText(name)&& hasText(website)){
                String NAME = name.getText().toString();
                String WEBSITE = website.getText().toString();
                String URL = "https://cs496-final-proj.appspot.com/supplier";

                Log.d(LOM, "Submit clicked :" + NAME + " " + WEBSITE);

                String params ="";
                if(Patterns.WEB_URL.matcher(WEBSITE).matches()){
                    params = createParams(NAME, WEBSITE);
                    ConnectoinTask supplierTask = new ConnectoinTask();
                    supplierTask.execute(URL, params, "POST");

                    /*Intent intent = new Intent();
                    intent.putExtra("app.android.chemicals.Data", "Ok");
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                    getFragmentManager().popBackStack();*/
                    /** Option to automatically return on previous screen
                     FragmentManager manager = getActivity().getSupportFragmentManager();
                     FragmentTransaction trans = manager.beginTransaction();
                     trans.remove(this);
                     trans.commit();
                     manager.popBackStack();
                     */
                }//if
                else {
                    Toast toast = Toast.makeText(this.getActivity(),
                            "URL is NOT valid! Try again!", Toast.LENGTH_LONG);
                    View view = toast.getView();
                    view.setBackgroundResource(R.drawable.red_toast);
                    //TextView text = (TextView) view.findViewById(android.R.id.message);
                    /*here you can do anything with text*/
                    toast.show();
                    //Toast.makeText(this.getActivity(),
                    //    "URL is NOT valid! Try again!", Toast.LENGTH_LONG).show();
                }
            }//if
        }
        else
            Toast.makeText(this.getActivity(),
                "No internet connection!\n" +
                "Turn on connection and continue", Toast.LENGTH_LONG).show();

    }//onClick()

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
            final String SUPPLIERS = "suppliers";
            final String NAME = "name";
            final String WEBSITE = "website";
            final String KEY = "key";

            // Raw JSON String to JSON object
            JSONObject suppliersJson = new JSONObject(suppliersJsonStr);

            // String array to save each supplier
            String resultStr = "";
            // Strings to save data
            String name;
            String website;
            String key;
            // Get the JSON object representing the supplier

            name = suppliersJson.getString(NAME);
            website = suppliersJson.getString(WEBSITE);
            key = suppliersJson.getString(KEY);

            resultStr = name; //+ "\n" + website + "\n" + key;

                Log.v(LOG_TAG, "Supplier entry: " + resultStr);
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

                //http://stackoverflow.com/questions/1968416/how-to-do-http-authentication-in-android
                //http://developer.android.com/reference/java/net/HttpURLConnection.html
                String credentials = USER_ID + ":" + TOKEN;
                Log.v(LOG_TAG, "!!!Built credentials!!!" + credentials);
                String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP); //or .DEFAULT

                // Create the request to cloud and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(method);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Authorization", "basic " + base64EncodedCredentials); //<---------------
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

                Log.v(LOG_TAG, "Supplier string Response from BackEnd:\n" + suppliersJsonStr);
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
                result = "Added Supplier to the table: \n"+result;
                name.setText("");
                website.setText("");

                Toast toast = Toast.makeText(getActivity(), result, Toast.LENGTH_LONG);
                View view = toast.getView();
                view.setBackgroundResource(R.drawable.red_toast);
                toast.show();
            }
        }
    }

}