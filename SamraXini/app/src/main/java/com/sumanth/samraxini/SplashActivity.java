package com.sumanth.samraxini;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Make sure you have a 'activity_splash.xml' layout

        // Use a Handler to delay the start of the main activity, showing the splash screen.
        new Handler().postDelayed(this::requestPermissions, 1500); // 1.5-second delay
    }

    private void requestPermissions() {
        // Use Dexter to request all dangerous permissions at once.
        Dexter.withContext(this)
                .withPermissions(
                        // List all dangerous permissions your app needs here
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS // Required for Android 13+ notifications
                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // This method is called when the user responds to the permission dialog.
                        if (report.areAllPermissionsGranted()) {
                            // If all permissions are granted, proceed to the main part of the app.
                            Toast.makeText(SplashActivity.this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                            startMainActivity();
                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            // If the user permanently denied a permission, you should show a dialog
                            // guiding them to the app settings. For now, we'll show a Toast.
                            Toast.makeText(SplashActivity.this, "Some permissions are permanently denied. Please enable them in app settings.", Toast.LENGTH_LONG).show();
                            // You might want to close the app or show a limited functionality screen here.
                            startMainActivity(); // Or finish();
                        } else {
                            // If some permissions were denied but not permanently.
                            Toast.makeText(SplashActivity.this, "Some permissions were denied.", Toast.LENGTH_SHORT).show();
                            startMainActivity(); // Or ask again, or finish().
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        // This is called if a user has denied a permission before.
                        // You can show an explanation of why you need the permissions.
                        token.continuePermissionRequest(); // For now, we'll just continue with the request.
                    }
                })
                .check(); // This starts the permission request process.
    }

    private void startMainActivity() {
        // This method starts the main activity after the permission check is complete.
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Call finish() to remove the splash screen from the back stack.
    }
}
