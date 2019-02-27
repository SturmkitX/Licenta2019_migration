package com.example.licentaproject;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

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
                    tx.replace(R.id.fragment, new BlankFragment2());
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
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d("Tag_ID", tag.toString());
    }
}
