package com.example.licentaproject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import models.AuthResponse;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void logUserIn(View view) {
        String username = ((EditText) findViewById(R.id.emailField)).getText().toString();
        String password = ((EditText) findViewById(R.id.passField)).getText().toString();

        Log.d("username", username);
        Log.d("password", password);

        new RequestTask(this).execute(username, password);
    }

    private class RequestTask extends AsyncTask<String, Void, AuthResponse> {

        private Context context;

        public RequestTask(Context context) {
            this.context = context;
        }

        @Override
        protected AuthResponse doInBackground(String... params) {
            HttpURLConnection conn = null;
            AuthResponse result = null;
            // send a HTTP request
            try {
                conn = (HttpURLConnection) new URL("http://192.168.0.105:3000/public/login").openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                Map<String, String> credMap = new HashMap<>();
                credMap.put("email", params[0]);
                credMap.put("password", params[1]);
                ObjectMapper mapper = new ObjectMapper();
                byte[] reqBytes = mapper.writeValueAsBytes(credMap);
                conn.setRequestProperty("Content-Length", "" + reqBytes.length);

                OutputStream out = new BufferedOutputStream(conn.getOutputStream());


                out.write(mapper.writeValueAsBytes(credMap));

                Log.d("credentials", mapper.writeValueAsString(credMap));

                out.close();

                // Get the response
                result = mapper.readValue(conn.getInputStream(), AuthResponse.class);

                Log.d("LoginResponse", result.toString());
                conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        protected void onPostExecute(AuthResponse auth) {
            Intent intent = new Intent(context, NavActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
