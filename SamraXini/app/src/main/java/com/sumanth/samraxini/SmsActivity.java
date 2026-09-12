package com.sumanth.samraxini;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class SmsActivity extends AppCompatActivity {
    Button start, stop, helpline, activateVoice;
    boolean isVoiceActive = false; // Manages the state of the voice button

    /**
     * This method is removed as it creates a confusing navigation loop.
     * The default back button behavior is what users expect.
     *
     * @Override
     * public void onBackPressed() {
     *     super.onBackPressed();
     *     startActivity(new Intent(SmsActivity.this, MainActivity.class));
     * }
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        stop = findViewById(R.id.stopService);
        start = findViewById(R.id.startService);
        helpline = findViewById(R.id.btn_helpline);
        activateVoice = findViewById(R.id.activateVoice);

        start.setOnClickListener(this::startServiceV);
        stop.setOnClickListener(this::stopService);
        helpline.setOnClickListener(this::helplines);

        // FIX: The click listener now calls the corrected method.
        activateVoice.setOnClickListener(this::toggleVoiceService);
    }

    public void helplines(View view) {
        startActivity(new Intent(SmsActivity.this, HelplineCall.class));
    }

    // FIX: This method is simplified. It sends a "stop" signal to the service,
    // and the service itself will handle its shutdown logic.
    public void stopService(View view) {
        Intent serviceIntent = new Intent(this, ServiceMine.class);
        serviceIntent.setAction("stop"); // Set the action for the service
        startService(serviceIntent); // Use startService for all versions; it correctly routes to startForegroundService on newer APIs if needed
        Snackbar.make(findViewById(android.R.id.content), "Service stop signal sent.", Snackbar.LENGTH_LONG).show();
    }

    public void startServiceV(View view) {
        // First, check for the "draw over other apps" permission, which is required for shake detection UI.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            Toast.makeText(this, "Please grant the overlay permission for the app to work correctly.", Toast.LENGTH_LONG).show();
            startActivity(intent);
            return; // Exit the method until permission is granted.
        }

        // FIX: Replaced redundant permission checks. These should be handled by Dexter in SplashActivity.
        // If the service needs these permissions, it should verify them itself.
        // This simplifies the Activity code.
        Intent serviceIntent = new Intent(this, ServiceMine.class);
        serviceIntent.setAction("start"); // Set the action for the service. Note: "start" is lowercase.
        startService(serviceIntent);
        Snackbar.make(findViewById(android.R.id.content), "Service start signal sent.", Snackbar.LENGTH_LONG).show();
    }

    // FIX: This method now correctly sends an Intent to the service instead of calling a static method.
    public void toggleVoiceService(View view) {
        isVoiceActive = !isVoiceActive;

        if (isVoiceActive) {
            // Send an intent with a custom "startVoice" action to the service.
            sendActionToService("startVoice");
            activateVoice.setText("Deactivate Voice"); // Update the button's text
            Toast.makeText(this, "Voice recognition activated", Toast.LENGTH_SHORT).show();
        } else {
            // Send an intent with a different "stopVoice" action.
            sendActionToService("stopVoice");
            activateVoice.setText("Activate Voice"); // Reset the button's text
            Toast.makeText(this, "Voice recognition deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    // FIX: A new helper method to create and send an Intent to the service.
    // This avoids code duplication and is much cleaner.
    private void sendActionToService(String action) {
        Intent serviceIntent = new Intent(this, ServiceMine.class);
        serviceIntent.setAction(action);
        startService(serviceIntent);
    }
}
