package app.android.chemicals;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Nick on 6/7/2015.
 */

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
public abstract class FetchSuppliers extends AsyncTask<Object, Void, Supplier[]> {
        //public class FetchSupplierTask extends AsyncTask<Void, Void, String[]> {
        // get the name of the name of class


        private final String LOG_TAG = FetchSuppliers.class.getSimpleName();

        /**
         * getSupplierDataFromJson()
         *  Parses JSON to get name, website and key
         *  parameters: JSON string
         *  return: String of suppliers
         */
        private Supplier[] getSupplierDataFromJson(String suppliersJsonStr)
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
            Supplier[] resultStrs = new Supplier[suppliersArray.length()];
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

                //resultStrs[i] = name + "\n" + website + "\n" + key;
                Supplier supp = new Supplier();
                supp.id = key;
                supp.name = name;
                supp.website = website;
                resultStrs[i] = supp;

            }

            for (Supplier s : resultStrs) {
                Log.v(LOG_TAG, "Supplier name: " + s.name);
            }
            return resultStrs;

        }

        @Override
        protected Supplier[] doInBackground(Object... params) {

            String url_string = (String) params[0];
            //String name_value = (String) params[1];
            String method = (String) params[1];
            String USER_ID = (String) params[2];
            String TOKEN  = (String) params[3];

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
        }//doInBackground()

    }//FetchSupplierTask class

