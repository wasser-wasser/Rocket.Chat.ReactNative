package fdroid_up.rocket.reactnative.notification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.os.AsyncTask;
import java.io.OutputStream;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UnifiedPushServerRegister {
    private int RETRY_COUNT = 0;
    private int[] TIMEOUT = new int[]{0, 1, 3, 5, 10};
    private String TOKEN_KEY = "reactnativemeteor_usertoken-";

    public interface Callback {
        void onResponse(String result);
        void onError(Exception error);
    }
    
    public static void sendPost(Context context, String urlString, String jsonBody, Callback callback) {
        Log.d("UnifiedPushServerRegister", "UnifiedPushServerRegister..."+ urlString+"    "+ jsonBody);
        new PostTask(context, callback).execute(urlString, jsonBody);
    }

    private static class PostTask extends AsyncTask<String, Void, String> {
        private Context context;
        private Callback callback;
        private Exception error = null;

        public PostTask(Context context, Callback callback) {
            this.context = context.getApplicationContext();
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            String jsonBody = params[1];

            HttpURLConnection connection = null;

            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.close();

                int responseCode = connection.getResponseCode();
                Log.d("UnifiedPushServerRegister", "Response Code: " + responseCode);

                InputStream inputStream = (responseCode < HttpURLConnection.HTTP_BAD_REQUEST)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                return response.toString();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("UnifiedPushServerRegister", "Error in POST request", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (callback != null) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onResponse(result);
                }
            }
        }
    }
}

