package com.sumanth.samraxini;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    FusedLocationProviderClient fusedLocationClient;
    String myLocation = "", numberCall;
    SmsManager manager = SmsManager.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        findViewById(R.id.panicBtn).setOnClickListener(this);
        findViewById(R.id.fourth).setOnClickListener(this);
        findViewById(R.id.first).setOnClickListener(this);
        findViewById(R.id.second).setOnClickListener(this);
        findViewById(R.id.fifth).setOnClickListener(this);
        findViewById(R.id.third).setOnClickListener(this);

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.RECORD_AUDIO
        };

        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.fourth) {
            startActivity(new Intent(MainActivity.this, LawsActivity.class));
            finish();
        } else if (id == R.id.first) {
            startActivity(new Intent(MainActivity.this, ContactActivity.class));
            finish();
        } else if (id == R.id.fifth) {
            startActivity(new Intent(MainActivity.this, SelfDefenseActivity.class));
        } else if (id == R.id.second) {
            startActivity(new Intent(MainActivity.this, SmsActivity.class));
            finish();
        } else if (id == R.id.third) {
            startActivity(new Intent(MainActivity.this, SafetyPredictionActivity.class));
            finish();
        } else if (id == R.id.panicBtn) {
            triggerEmergencyActions();
        }
    }

    private void triggerEmergencyActions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        myLocation = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                    } else {
                        myLocation = "Unable to Find Location :(";
                    }
                    sendMsg();
                });

        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        numberCall = sharedPreferences.getString("firstNumber", "None");

        if (!numberCall.equalsIgnoreCase("None")) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + numberCall));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Call permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMsg() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        Set<String> oldNumbers = sharedPreferences.getStringSet("enumbers", new HashSet<>());

        if (!oldNumbers.isEmpty()) {
            for (String ENUM : oldNumbers) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    manager.sendTextMessage(ENUM, null, "Hello, I am in Trouble!\nAm sharing my current Location :\n" + myLocation, null, null);
                } else {
                    Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied: " + permissions[i], Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}