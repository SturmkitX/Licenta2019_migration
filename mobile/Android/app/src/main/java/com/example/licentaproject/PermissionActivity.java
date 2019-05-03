package com.example.licentaproject;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private final int reqCode = 10;
    private int denyCount = 0;

    private void checkPermissions() {
        Log.d("PERM_CHECK", "Checking permissions...");
//        boolean fineLocation = PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
//        boolean coarseLocation = PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
//        boolean success = fineLocation && coarseLocation;
//
//        Log.d("PERM_STATUS", "Success is: " + success);

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, reqCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        checkPermissions();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case reqCode: {
                boolean success = true;
                for (int status : grantResults) {
                    success &= (status == PackageManager.PERMISSION_GRANTED);
                }

                if (!success) {
                    denyCount++;
                    if (denyCount == 2) {
                        Toast.makeText(this, "Please allow the location service in the settings!", Toast.LENGTH_SHORT).show();
                    } else {
                        checkPermissions();
                    }
                } else {
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            } break;
        }
    }
}
