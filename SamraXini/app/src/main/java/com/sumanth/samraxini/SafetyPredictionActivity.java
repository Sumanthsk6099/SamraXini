package com.sumanth.samraxini;

import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

public class SafetyPredictionActivity extends AppCompatActivity {

    private Spinner locationSpinner;
    private TimePicker timePicker;
    private TextView safetyResultTextView;
    private Map<String, Map<String, String>> safetyData = new HashMap<>();

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SafetyPredictionActivity.this,MainActivity.class));
        SafetyPredictionActivity.this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.predict_safety);

        locationSpinner = findViewById(R.id.locationSpinner);
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(false);
        safetyResultTextView = findViewById(R.id.safetyResultTextView);
        Button predictSafetyButton = findViewById(R.id.predictSafetyButton);

        loadSafetyData();
        setupLocationSpinner();

        predictSafetyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String location = locationSpinner.getSelectedItem().toString().trim();
                String timePeriod = getTimePeriod(timePicker.getHour(), timePicker.getMinute()).trim();
                if (safetyData.containsKey(location) && safetyData.get(location).containsKey(timePeriod)) {
                    String safetyLevel = safetyData.get(location).get(timePeriod);
                    safetyResultTextView.setText(safetyLevel);
                    safetyResultTextView.setVisibility(View.VISIBLE);
                } else {
                    safetyResultTextView.setText("Safety information unavailable");
                    safetyResultTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadSafetyData() {
        try {
            XmlResourceParser parser = getResources().getXml(R.xml.location_safety);
            String locationName = null;
            Map<String, String> timeSafetyMap = null;
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("location".equals(tagName)) {
                            locationName = parser.getAttributeValue(null, "name");
                            timeSafetyMap = new HashMap<>();
                        } else if ("time".equals(tagName) && locationName != null) {
                            String period = parser.getAttributeValue(null, "period");
                            String safety = parser.getAttributeValue(null, "safety");
                            timeSafetyMap.put(period, safety);
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if ("location".equals(tagName) && locationName != null && timeSafetyMap != null) {
                            safetyData.put(locationName, timeSafetyMap);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLocationSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.select_location, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
    }

    private String getTimePeriod(int hour, int minute) {
        if ((hour == 6 && minute >= 1) || (hour > 6 && hour < 11) || (hour == 11 && minute == 0)) {
            return "Morning";
        } else if ((hour == 11 && minute >= 1) || (hour > 11 && hour < 16) || (hour == 16 && minute == 0)) {
            return "Afternoon";
        } else if ((hour == 16 && minute >= 1) || (hour > 16 && hour < 20) || (hour == 20 && minute == 0)) {
            return "Evening";
        } else {
            return "Night";
        }
    }
}
