package com.flop.lockedtolearn.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.flop.lockedtolearn.R;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);

        Bundle b = getIntent().getExtras();
        String statsStr = b.getString("game");

        TextView statsView = findViewById(R.id.stats_field);
        statsView.setText(statsStr);
    }

    public void back(View view){
        finish();
    }
}
