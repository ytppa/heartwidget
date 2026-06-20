package com.ytppa.nothingheart;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class HeartSettingsActivity extends Activity {
    private HeartRepository repository;
    private TextView sentBeatCountValue;
    private TextView receivedBeatCountValue;
    private TextView pairingValue;
    private EditText partnerPairCodeInput;
    private Button createIdentityButton;
    private Button requestPairingButton;
    private Button completePairingButton;
    private Button resetPairingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_settings);

        repository = HeartRepositories.get(this);
        sentBeatCountValue = findViewById(R.id.sent_beat_count_value);
        receivedBeatCountValue = findViewById(R.id.received_beat_count_value);
        pairingValue = findViewById(R.id.pairing_value);
        partnerPairCodeInput = findViewById(R.id.partner_pair_code_input);
        createIdentityButton = findViewById(R.id.create_identity_button);
        requestPairingButton = findViewById(R.id.request_pairing_button);
        completePairingButton = findViewById(R.id.complete_pairing_button);
        resetPairingButton = findViewById(R.id.reset_pairing_button);
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

        requestPairingButton.setOnClickListener(view -> {
            requestPairing();
            updateUi();
        });

        completePairingButton.setOnClickListener(view -> {
            repository.completeLocalPairing();
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
        updatePairingControls(pairingState);
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
                    formatPartner(pairingState)
            );
        }
        if (pairingState.getPairStatus() == HeartPairingStatus.PENDING) {
            return getString(
                    R.string.pairing_state_pending,
                    pairingState.getPairCode(),
                    pairingState.getPartnerPairCode()
            );
        }
        return getString(R.string.pairing_state_none, pairingState.getPairCode());
    }

    private void updatePairingControls(HeartPairingState pairingState) {
        boolean hasLocalIdentity = pairingState.hasLocalIdentity();
        boolean isPending = pairingState.getPairStatus() == HeartPairingStatus.PENDING;
        boolean isPaired = pairingState.getPairStatus() == HeartPairingStatus.PAIRED;

        createIdentityButton.setEnabled(!hasLocalIdentity);
        partnerPairCodeInput.setEnabled(hasLocalIdentity && !isPaired);
        requestPairingButton.setEnabled(hasLocalIdentity && !isPaired);
        completePairingButton.setEnabled(isPending);
        resetPairingButton.setEnabled(hasLocalIdentity && (isPending || isPaired));

        if (!partnerPairCodeInput.hasFocus()) {
            String visiblePartnerCode = pairingState.getPartnerPairCode();
            if (!visiblePartnerCode.equals(partnerPairCodeInput.getText().toString())) {
                partnerPairCodeInput.setText(visiblePartnerCode);
            }
        }
    }

    private void requestPairing() {
        HeartPairingState pairingState = repository.getPairingState();
        if (!pairingState.hasLocalIdentity()) {
            Toast.makeText(this, R.string.pairing_error_missing_identity, Toast.LENGTH_SHORT).show();
            return;
        }

        String partnerPairCode = HeartStateStore.normalizePairCode(partnerPairCodeInput.getText().toString());
        partnerPairCodeInput.setText(partnerPairCode);

        if (!HeartStateStore.isValidPairCode(partnerPairCode)) {
            Toast.makeText(this, R.string.pairing_error_invalid_code, Toast.LENGTH_SHORT).show();
            return;
        }
        if (partnerPairCode.equals(pairingState.getPairCode())) {
            Toast.makeText(this, R.string.pairing_error_own_code, Toast.LENGTH_SHORT).show();
            return;
        }

        repository.requestPairing(partnerPairCode);
    }

    private String formatPartner(HeartPairingState pairingState) {
        if (pairingState.hasPartnerPairCode()) {
            return pairingState.getPartnerPairCode();
        }
        return pairingState.getPartnerId();
    }
}
