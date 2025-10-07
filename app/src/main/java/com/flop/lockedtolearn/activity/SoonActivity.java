package com.flop.lockedtolearn.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.flop.lockedtolearn.R;

public class SoonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.soon);
    }

    public void back(View view){
        finish();
    }
}
