package com.flop.lockedtolearn.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.flop.lockedtolearn.R;
import com.flop.lockedtolearn.game.Game;
import com.flop.lockedtolearn.service.LockscreenService;
import com.google.android.material.textfield.TextInputEditText;

public class AddWordsActivity extends AppCompatActivity {

    private Game game;
    boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_word);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        this.game = new Game(getApplicationContext(), vibrator);
    }

    public void back(View view) {
        finish();
    }

    public void onAdd(View view) {
        TextInputEditText nativeWordInput = findViewById(R.id.word_input);
        TextInputEditText translationInput = findViewById(R.id.translation_input);

        Editable nativeWord = nativeWordInput.getText();
        Editable translation = translationInput.getText();

        if (nativeWord == null || translation == null) {
            Toast.makeText(getApplicationContext(), "Both inputs must not be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        String nativeWordStr = nativeWord.toString();
        String translationStr = translation.toString();

        if (nativeWordStr.isBlank() || translationStr.isBlank()) {
            Toast.makeText(getApplicationContext(), "Both inputs must not be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        this.game.addPair(nativeWordStr, translationStr);

        Intent intent = new Intent(AddWordsActivity.this, LockscreenService.class);

        if (this.isServiceRunning) {
            stopService(intent);
            startService(intent);
        }

        nativeWordInput.setText("");
        translationInput.setText("");

        Toast.makeText(getApplicationContext(), "Word added successfully", Toast.LENGTH_SHORT).show();
    }
}
