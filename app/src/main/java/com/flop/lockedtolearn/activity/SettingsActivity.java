package com.flop.lockedtolearn.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.flop.lockedtolearn.R;
import com.flop.lockedtolearn.game.Game;
import com.flop.lockedtolearn.service.LockscreenService;

public class SettingsActivity extends AppCompatActivity {

    private Game game;
    boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        this.game = new Game(getApplicationContext(), vibrator);

        this.game.readSettings();

        Bundle b = getIntent().getExtras();
        this.isServiceRunning = b.getBoolean("isRunning");

        SwitchCompat reverseSwitch = findViewById(R.id.reverse_mode);
        reverseSwitch.setChecked(this.game.getAllowReverse());
    }

    public void back(View view){
        finish();
    }

    public void toggleReverse(View view) {
        this.game.toggleAllowReverse();

        Intent intent = new Intent(SettingsActivity.this, LockscreenService.class);

        if (this.isServiceRunning) {
            stopService(intent);
            startService(intent);
        }
    }
}
