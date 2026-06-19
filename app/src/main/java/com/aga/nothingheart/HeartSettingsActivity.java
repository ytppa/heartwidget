package com.aga.nothingheart;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class HeartSettingsActivity extends Activity {
    private TextView sentBeatCountValue;
    private TextView receivedBeatCountValue;
    private TextView pairingValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_settings);

        sentBeatCountValue = findViewById(R.id.sent_beat_count_value);
        receivedBeatCountValue = findViewById(R.id.received_beat_count_value);
        pairingValue = findViewById(R.id.pairing_value);
        Button sendBeatButton = findViewById(R.id.send_beat_button);
        Button simulateIncomingBeatButton = findViewById(R.id.simulate_incoming_beat_button);
        Button simulateManyIncomingBeatsButton = findViewById(R.id.simulate_many_incoming_beats_button);
        Button resetReceivedButton = findViewById(R.id.reset_received_count_button);
        Button resetAllButton = findViewById(R.id.reset_all_counts_button);
        Button refreshButton = findViewById(R.id.refresh_widget_button);

        sendBeatButton.setOnClickListener(view -> {
            HeartStateStore.incrementSentBeatCount(this);
            HeartStateStore.resetReceivedBeatCount(this);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        simulateIncomingBeatButton.setOnClickListener(view -> {
            HeartStateStore.incrementReceivedBeatCount(this);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        simulateManyIncomingBeatsButton.setOnClickListener(view -> {
            HeartStateStore.addReceivedBeatCount(this, 1000);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        resetReceivedButton.setOnClickListener(view -> {
            HeartStateStore.resetReceivedBeatCount(this);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        resetAllButton.setOnClickListener(view -> {
            HeartStateStore.resetAllBeatCounts(this);
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
        int sentBeatCount = HeartStateStore.getSentBeatCount(this);
        int receivedBeatCount = HeartStateStore.getReceivedBeatCount(this);

        sentBeatCountValue.setText(formatBeatCountDisplay(sentBeatCount));
        receivedBeatCountValue.setText(formatBeatCountDisplay(receivedBeatCount));
        pairingValue.setText(R.string.pairing_local_only);
    }

    private String formatBeatCountDisplay(int beatCount) {
        String formattedCount = HeartStateStore.formatBeatCount(beatCount);
        return getString(R.string.beat_count_display, beatCount, formattedCount.isEmpty() ? "0" : formattedCount);
    }
}
