package com.example.licentaproject;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.example.licentaproject.models.User;
import com.example.licentaproject.utils.HttpRequestUtil;
import com.example.licentaproject.utils.SessionData;

public class NavActivity extends AppCompatActivity {

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentTransaction tx = null;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    tx = getSupportFragmentManager().beginTransaction();
                    tx.replace(R.id.fragment, new BlankFragment());
                    tx.commit();
                    return true;
                case R.id.navigation_dashboard:
                    tx = getSupportFragmentManager().beginTransaction();
//                    tx.replace(R.id.fragment, new BlankFragment2());
                    tx.commit();
                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.add(R.id.fragment, new BlankFragment());
        tx.commit();

        Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d("TAG_CREATE", "Tag is in onCreate: " + (tag == null ? "NO" : "YES"));

        new UserRequestTask().execute();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private class UserRequestTask extends AsyncTask<Object, Void, User> {

        @Override
        protected User doInBackground(Object... params) {
            return (User) HttpRequestUtil.sendRequest("resource/me/user", "GET", null, User.class, false);
        }

        protected void onPostExecute(User user) {
            if (user == null) {
                Log.d("AUTH_USER_DATA_STATUS", "User fetch error!");
            } else {
                Log.d("AUTH_USER_DATA_STATUS", "User fetch successful");
                Log.d("USER_FETCH_ID", user.getId());
                SessionData.setUser(user);
            }
        }
    }
}
