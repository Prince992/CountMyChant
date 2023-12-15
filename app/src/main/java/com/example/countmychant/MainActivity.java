package com.example.countmychant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Spinner mantraSpinner;
    private EditText countInput;
    private Button startChantingButton;
    private TextView currentCountLabel;
    private TextView recognizedMantraLabel;

    private SpeechRecognizer speechRecognizer;
    private boolean chantingStarted = false;
    private int targetCount = 0;
    private int currentCount = 0;
    private String selectedMantra;

    private static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mantraSpinner = findViewById(R.id.mantraSpinner);
        countInput = findViewById(R.id.countInput);
        startChantingButton = findViewById(R.id.startChantingButton);
        currentCountLabel = findViewById(R.id.currentCountLabel);
        recognizedMantraLabel = findViewById(R.id.recognizedMantraLabel);

        currentCount = 0;
        updateCurrentCount();

        String recognizedText = "";
        recognizedMantraLabel.setText(getString(R.string.recognized_mantra_label, recognizedText));

        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(v -> resetApp());

        String[] mantraOptions = {"Hello", "Shri Radhe", "Om Namah Shivay", "Om Namo Narayan"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mantraOptions);
        mantraSpinner.setAdapter(adapter);

        startChantingButton.setOnClickListener(v -> {
            if (!chantingStarted) {
                requestAudioPermissionAndStartChanting();
            } else {
                stopChanting();
            }
        });
    }

    private void requestAudioPermissionAndStartChanting() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            startChanting();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startChanting();
            } else {
                Toast.makeText(this, "Permission denied. Cannot start chanting.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startChanting() {
        selectedMantra = mantraSpinner.getSelectedItem().toString();
        String targetCountString = countInput.getText().toString();

        if (targetCountString.isEmpty()) {
            Toast.makeText(this, "Please set a target count before starting chanting.", Toast.LENGTH_SHORT).show();
            return;
        }

        targetCount = Integer.parseInt(targetCountString);
        currentCount = 0;
        updateCurrentCount();

        String recognizedText = "";
        recognizedMantraLabel.setText(getString(R.string.recognized_mantra_label, recognizedText));

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float msdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {}

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String result = TextUtils.join(" ", matches);

                    updateRecognizedMantra(result);

                    int mantraOccurrences = countMantraOccurrences(result, selectedMantra);
                    currentCount += mantraOccurrences;
                    if(currentCount >= targetCount)
                    {
                        currentCount = targetCount;
                    }
                    updateCurrentCount();

                    if (currentCount >= targetCount) {
                        stopChanting();
                    } else {
                        speechRecognizer.startListening(intent);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        chantingStarted = true;
        startChantingButton.setText(R.string.stop_chanting_button_text);
        speechRecognizer.startListening(intent);
    }

    private int countMantraOccurrences(String text, String mantra) {
        int count = 0;
        int index = text.toLowerCase().indexOf(mantra.toLowerCase());

        while (index != -1) {
            count++;
            index = text.toLowerCase().indexOf(mantra.toLowerCase(), index + mantra.length());
        }

        return count;
    }

    private void updateRecognizedMantra(String mantra) {
        recognizedMantraLabel.setText(getString(R.string.recognized_mantra_label, mantra));
    }

    private void stopChanting() {
        speechRecognizer.stopListening();
        chantingStarted = false;
        startChantingButton.setText(R.string.start_chanting_button_text);
        updateCurrentCount();
        if (currentCount >= targetCount) {
            Toast.makeText(this, "Target count reached! You may stop.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCurrentCount() {
        currentCountLabel.setText(getString(R.string.current_count_label, currentCount));
    }

    private void resetApp() {
        currentCount = 0;
        updateCurrentCount();
        String recognizedText = "";
        recognizedMantraLabel.setText(getString(R.string.recognized_mantra_label, recognizedText));
        startChantingButton.setEnabled(true);
        countInput.getText().clear();

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        chantingStarted = false;
        startChantingButton.setText(R.string.start_chanting_button_text);
    }

}
