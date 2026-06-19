package com.aga.nothingheart;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class HeartSettingsActivity extends Activity {
    private HeartRepository repository;
    private TextView sentBeatCountValue;
    private TextView receivedBeatCountValue;
    private TextView pairingValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_settings);

        repository = HeartRepositories.get(this);
        sentBeatCountValue = findViewById(R.id.sent_beat_count_value);
        receivedBeatCountValue = findViewById(R.id.received_beat_count_value);
        pairingValue = findViewById(R.id.pairing_value);
        Button createIdentityButton = findViewById(R.id.create_identity_button);
        Button simulatePendingPairingButton = findViewById(R.id.simulate_pending_pairing_button);
        Button simulatePairedPartnerButton = findViewById(R.id.simulate_paired_partner_button);
        Button resetPairingButton = findViewById(R.id.reset_pairing_button);
        Button sendBeatButton = findViewById(R.id.send_beat_button);
        Button simulateIncomingBeatButton = findViewById(R.id.simulate_incoming_beat_button);
        Button simulateManyIncomingBeatsButton = findViewById(R.id.simulate_many_incoming_beats_button);
        Button resetReceivedButton = findViewById(R.id.reset_received_count_button);
        Button resetAllButton = findViewById(R.id.reset_all_counts_button);
        Button refreshButton = findViewById(R.id.refresh_widget_button);

        createIdentityButton.setOnClickListener(view -> {
            repository.ensureLocalIdentity();
            updateUi();
        });

        simulatePendingPairingButton.setOnClickListener(view -> {
            repository.simulatePendingPairing();
            updateUi();
        });

        simulatePairedPartnerButton.setOnClickListener(view -> {
            repository.simulatePairedPartner();
            updateUi();
        });

        resetPairingButton.setOnClickListener(view -> {
            repository.resetPairing();
            updateUi();
        });

        sendBeatButton.setOnClickListener(view -> {
            repository.sendBeatToPartner();
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        simulateIncomingBeatButton.setOnClickListener(view -> {
            repository.simulateIncomingBeat();
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        simulateManyIncomingBeatsButton.setOnClickListener(view -> {
            repository.simulateIncomingBeats(1000);
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        resetReceivedButton.setOnClickListener(view -> {
            repository.clearReceivedBeats();
            HeartWidgetProvider.refreshAllWidgets(this);
            updateUi();
        });

        resetAllButton.setOnClickListener(view -> {
            repository.resetAllBeatCounts();
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
        int sentBeatCount = repository.getSentBeatCount();
        int receivedBeatCount = repository.getReceivedBeatCount();
        HeartPairingState pairingState = repository.getPairingState();

        sentBeatCountValue.setText(formatBeatCountDisplay(sentBeatCount));
        receivedBeatCountValue.setText(formatBeatCountDisplay(receivedBeatCount));
        pairingValue.setText(formatPairingDisplay(pairingState));
    }

    private String formatBeatCountDisplay(int beatCount) {
        String formattedCount = HeartStateStore.formatBeatCount(beatCount);
        return getString(R.string.beat_count_display, beatCount, formattedCount.isEmpty() ? "0" : formattedCount);
    }

    private String formatPairingDisplay(HeartPairingState pairingState) {
        if (!pairingState.hasLocalIdentity()) {
            return getString(R.string.pairing_state_missing);
        }
        if (pairingState.getPairStatus() == HeartPairingStatus.PAIRED) {
            return getString(
                    R.string.pairing_state_paired,
                    pairingState.getPairCode(),
                    pairingState.getPartnerId()
            );
        }
        if (pairingState.getPairStatus() == HeartPairingStatus.PENDING) {
            return getString(R.string.pairing_state_pending, pairingState.getPairCode());
        }
        return getString(R.string.pairing_state_none, pairingState.getPairCode());
    }
}
