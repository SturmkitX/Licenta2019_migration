package com.example.licentaproject;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.licentaproject.models.AuthResponse;
import com.example.licentaproject.utils.HttpRequestUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }

    public void registerUser(View view) {
        String firstName = ((EditText) findViewById(R.id.firstNameField)).getText().toString();
        String lastName = ((EditText) findViewById(R.id.lastNameField)).getText().toString();
        String email = ((EditText) findViewById(R.id.emailField)).getText().toString();
        String password = ((EditText) findViewById(R.id.passwordField)).getText().toString();

        // perform checks
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please specify all register information!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!Pattern.compile("\\S+@\\S+\\.\\S+").matcher(email).matches()) {
            Toast.makeText(this, "Please specify a valid e-mail address!", Toast.LENGTH_LONG).show();
            return;
        }

        new RegisterTask(this).execute(firstName, lastName, email, password);
    }

    private class RegisterTask extends AsyncTask<String, Void, Map> {

        private Context context;

        public RegisterTask(Context context) {
            this.context = context;
        }

        @Override
        protected Map doInBackground(String... params) {
            Map<String, String> credMap = new HashMap<>();
            credMap.put("firstName", params[0]);
            credMap.put("lastName", params[1]);
            credMap.put("email", params[2]);
            credMap.put("password", params[3]);
            return (Map) HttpRequestUtil.sendRequest("public/register", "POST", credMap, Map.class, false);
        }

        protected void onPostExecute(Map status) {
            if (status == null) {
                Log.d("REGISTER_STATUS", "Register failed!");
                Toast.makeText(getApplicationContext(), "Register failed, please try again!", Toast.LENGTH_LONG).show();
            } else {
                Log.d("REGISTER_STATUS", "Register successful");
                finish();
            }
        }
    }
}
