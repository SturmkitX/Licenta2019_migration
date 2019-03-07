package com.example.licentaproject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.example.licentaproject.utils.HttpRequestUtil;

import java.util.HashMap;
import java.util.Map;

import com.example.licentaproject.models.AuthResponse;
import com.example.licentaproject.utils.SessionData;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, TrackerMapActivity.class);
        startActivity(intent);
        finish();
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
            Map<String, String> credMap = new HashMap<>();
            credMap.put("email", params[0]);
            credMap.put("password", params[1]);
            return (AuthResponse) HttpRequestUtil.sendRequest("public/login", "POST", credMap, AuthResponse.class, false);
        }

        protected void onPostExecute(AuthResponse auth) {
            if (!auth.isAuth()) {
                Log.d("AUTH_STATUS", "Authentication failed!");
            } else {
                Log.d("AUTH_STATUS", "Authentication successful");
                SessionData.setToken(auth.getToken());
                Intent intent = new Intent(context, NavActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
