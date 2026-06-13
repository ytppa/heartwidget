package com.aga.nothingheart;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class HeartSettingsActivity extends Activity {
    private TextView beatCountValue;
    private TextView pairingValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_settings);

        beatCountValue = findViewById(R.id.beat_count_value);
        pairingValue = findViewById(R.id.pairing_value);
        Button addBeatButton = findViewById(R.id.add_beat_button);
        Button addManyBeatsButton = findViewById(R.id.add_many_beats_button);
        Button resetButton = findViewById(R.id.reset_count_button);
        Button refreshButton = findViewById(R.id.refresh_widget_button);

        addBeatButton.setOnClickListener(view -> {
            HeartStateStore.incrementLocalBeatCount(this);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        addManyBeatsButton.setOnClickListener(view -> {
            HeartStateStore.addLocalBeatCount(this, 1000);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        resetButton.setOnClickListener(view -> {
            HeartStateStore.resetLocalBeatCount(this);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        refreshButton.setOnClickListener(view -> {
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {
        int beatCount = HeartStateStore.getLocalBeatCount(this);
        String formattedCount = HeartStateStore.formatBeatCount(beatCount);
        beatCountValue.setText(getString(R.string.beat_count_display, beatCount, formattedCount.isEmpty() ? "0" : formattedCount));
        pairingValue.setText(R.string.pairing_local_only);
    }
}
