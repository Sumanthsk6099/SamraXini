package com.sumanth.samraxini;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.github.tbouron.shakedetector.library.ShakeDetector;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ServiceMine extends Service {

    // FIX: Removed 'static' from instance variables. A service is a singleton instance anyway.
    private boolean isRunning = false;
    private MediaPlayer mediaPlayer;
    private FusedLocationProviderClient fusedLocationClient;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private PowerManager.WakeLock wakeLock;

    // These can remain as they are
    private final SmsManager manager = SmsManager.getDefault();
    private String myLocation = "Location not available";

    private static final String CHANNEL_ID = "WSafetyChannel";
    private boolean isVoiceRecognitionActive = false; // Manages the voice recognition state

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // onCreate is called only once when the service is first created.
        // All initialization should go here.
        initializeServiceComponents();
    }

    // FIX: The core logic is now in onStartCommand to handle different actions.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            // If the service is restarted by the system, just keep it running.
            return START_STICKY;
        }

        String action = intent.getAction();
        switch (action) {
            case "start":
                startServiceLogic();
                break;
            case "stop":
                stopServiceLogic();
                break;
            case "startVoice":
                startVoiceRecognitionInternal();
                break;
            case "stopVoice":
                stopVoiceRecognitionInternal();
                break;
        }

        // Use START_STICKY to ensure the service restarts if it's killed by the system.
        return START_STICKY;
    }

    private void initializeServiceComponents() {
        createNotificationChannel();

        mediaPlayer = MediaPlayer.create(this, R.raw.siren);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize ShakeDetector but don't start it until the service is officially started.
        ShakeDetector.create(this, this::triggerSOSAlarm);
        ShakeDetector.updateConfiguration(5.0f, 3);

        initializeSpeechRecognizer();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WSafety::WakeLockTag");
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show();
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                // Restart listening only if voice recognition is supposed to be active
                if (isVoiceRecognitionActive) {
                    startVoiceRecognitionInternal();
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        if (match.toLowerCase(Locale.ROOT).contains("help") || match.toLowerCase(Locale.ROOT).contains("sos")) {
                            triggerSOSAlarm();
                            // No need to break, let it process other potential matches if any.
                        }
                    }
                }
                // Restart listening only if voice recognition is supposed to be active
                if (isVoiceRecognitionActive) {
                    startVoiceRecognitionInternal();
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startServiceLogic() {
        if (isRunning) {
            Toast.makeText(this, "Service is already running.", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = true;

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
        }

        ShakeDetector.start();
        updateLocation(); // Get an initial location fix.
        startForegroundServiceNotification();
        Toast.makeText(this, "Safety Service Started.", Toast.LENGTH_SHORT).show();
    }

    private void stopServiceLogic() {
        if (!isRunning) {
            return;
        }
        isRunning = false;

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            // Re-prepare the media player for next time
            try {
                mediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        stopVoiceRecognitionInternal();
        ShakeDetector.stop();

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        stopForeground(true);
        stopSelf(); // This will trigger onDestroy()
        Toast.makeText(this, "Safety Service Stopped.", Toast.LENGTH_SHORT).show();
    }

    private void triggerSOSAlarm() {
        // This method can only be called if the service is running.
        if (!isRunning) return;

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        updateLocation(); // Get the most recent location before sending.

        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        Set<String> emergencyNumbers = sharedPreferences.getStringSet("enumbers", new HashSet<>());

        if (emergencyNumbers.isEmpty()) {
            Toast.makeText(this, "No emergency contacts found!", Toast.LENGTH_SHORT).show();
        } else {
            for (String number : emergencyNumbers) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    String message = "I'm in trouble! Please help. My current location is:\n" + myLocation;
                    manager.sendTextMessage(number, null, message, null, null);
                } else {
                    // This case should ideally not happen if permissions are granted upfront.
                    Toast.makeText(this, "SMS permission not granted.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        Toast.makeText(this, "SOS ALARM TRIGGERED!", Toast.LENGTH_LONG).show();
    }

    // FIX: Renamed and made private. These are the internal implementations.
    private void startVoiceRecognitionInternal() {
        if (speechRecognizer != null) {
            isVoiceRecognitionActive = true;
            speechRecognizer.startListening(recognizerIntent);
            updateNotification("Voice command is active.");
        }
    }

    private void stopVoiceRecognitionInternal() {
        if (speechRecognizer != null) {
            isVoiceRecognitionActive = false;
            speechRecognizer.stopListening();
            updateNotification("Service is running."); // Reset notification text
        }
    }

    private void startForegroundServiceNotification() {
        Intent notificationIntent = new Intent(this, SmsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Spark Safety is Active")
                .setContentText("Service is running. Shake to trigger SOS.")
                .setSmallIcon(R.drawable.girlpower) // Ensure this drawable exists
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
    }

    // Helper to update the notification text
    private void updateNotification(String contentText) {
        Intent notificationIntent = new Intent(this, SmsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Spark Safety is Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.girlpower)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WSafety Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for the safety foreground service.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            myLocation = "Location permission not granted";
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                myLocation = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
            } else {
                myLocation = "Could not find location. Please ensure GPS is enabled.";
            }
        });
    }


    @Override
    public void onDestroy() {
        // This is the final cleanup method.
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // Stop the shake detector if it's still running
        ShakeDetector.destroy();
        isRunning = false;
        super.onDestroy();
    }
}
