package com.flop.lockedtolearn.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.flop.lockedtolearn.R;
import com.flop.lockedtolearn.game.Game;
import com.flop.lockedtolearn.service.LockscreenService;

public class MainActivity extends AppCompatActivity {

    boolean serviceRunning = false;

    TextView lockText;
    ImageView lockImage;
    TextView upload;
    TextView download;
    ConstraintLayout startCard;
    Game game;

    // random numbers used as identifier
    public static final int READ_PHONE_STATE_PERMISSION_REQUEST_CODE = 1;

    // overlay permission callback
    ActivityResultLauncher<Intent> permissionResultHandler = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            // new permissions retry starting service
            this.toggleService();
        }
    });

    ActivityResultLauncher<Intent> uploadARLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getData() != null) {
            Uri uri = result.getData().getData();
            this.upload.setText(R.string.loading);
            new Thread(() -> {
                String message = game.addFile(uri);
                runOnUiThread(() -> {
                    this.upload.setText(R.string.upload_btn);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

                    // restart service to load new game context
                    Intent intent = new Intent(MainActivity.this, LockscreenService.class);

                    if (this.isServiceRunning()) {
                        stopService(intent);
                        startService(intent);
                    }
                });
            }).start();
        }
    });

    ActivityResultLauncher<Intent> downloadARLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getData() != null) {
            Uri uri = result.getData().getData();
            this.download.setText(R.string.downloading);
            new Thread(() -> {
                String message = game.writeTemplate(uri);
                runOnUiThread(() -> {
                    this.download.setText(R.string.download_btn);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                });
            }).start();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        this.game = new Game(getApplicationContext(), vibrator);

        this.serviceRunning = isServiceRunning();

        this.lockText = findViewById(R.id.lock);
        this.lockText.setText(serviceRunning ? "Stop Learning" : "Start Learning");

        this.startCard = findViewById(R.id.start_card);
        this.startCard.setBackground(ContextCompat.getDrawable(getApplicationContext(), (serviceRunning ? R.drawable.stop_card_bg : R.drawable.start_card_bg)));

        this.lockImage = findViewById(R.id.lock_img);
        this.lockImage.setBackground(ContextCompat.getDrawable(getApplicationContext(), (serviceRunning ? R.drawable.stop : R.drawable.start)));

        this.upload = findViewById(R.id.upload);
        this.download = findViewById(R.id.download_template);
    }

    private void requestOverlayPermission() {
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
        this.permissionResultHandler.launch(intent);
    }

    private void requestPhonePermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_PERMISSION_REQUEST_CODE);
    }


    private boolean missingOverlayPermission() {
        return !Settings.canDrawOverlays(this);
    }

    private boolean missingPhonePermission() {
        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
        return permission != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case READ_PHONE_STATE_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.toggleService();
                } else {
                    Toast.makeText(getApplicationContext(), "Phone state permission was not granted. Please check your settings.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private boolean isServiceRunning() {
        return LockscreenService.isRunning;
    }

    public void onToggleLock(View view) {
        this.toggleService();
    }

    private void toggleService() {
        Intent intent = new Intent(MainActivity.this, LockscreenService.class);
        if (this.serviceRunning) {
            stopService(intent);
            this.serviceRunning = !this.serviceRunning;
        } else {
            if (missingOverlayPermission()) {
                requestOverlayPermission();
            }

            if (missingPhonePermission()) {
                requestPhonePermission();
            }

            if (missingPhonePermission() || missingOverlayPermission()) {
                return;
            }

            startService(intent);
            this.serviceRunning = !this.serviceRunning;
        }
        this.lockText.setText(this.serviceRunning ? "Stop Learning" : "Start Learning");
        this.startCard.setBackground(ContextCompat.getDrawable(getApplicationContext(), (this.serviceRunning ? R.drawable.stop_card_bg : R.drawable.start_card_bg)));
        this.lockImage.setBackground(ContextCompat.getDrawable(getApplicationContext(), (this.serviceRunning ? R.drawable.stop : R.drawable.start)));
    }

    public void onUploadFile(View view) {
        if (this.game.isLoading()) {
            return;
        }

        Intent file = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        file.setType("*/*");
        file = Intent.createChooser(file, "Choose a file");
        this.uploadARLauncher.launch(file);
    }

    public void onDownloadTemplate(View view) {
        if (this.game.isDownloading()) {
            return;
        }

        Intent file = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        file.setType("*/*.xls");
        file.putExtra(Intent.EXTRA_TITLE, "template.xls");
        file = Intent.createChooser(file, "Select a file path (.xls).");
        this.downloadARLauncher.launch(file);
    }

    public void openHowTo(View view) {
        startActivity(new Intent(MainActivity.this, HowToActivity.class));
    }

    public void openSoon(View view) {
        startActivity(new Intent(MainActivity.this, SoonActivity.class));
    }

    public void openSettings(View view) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

        Bundle b = new Bundle();
        b.putBoolean("isRunning", this.isServiceRunning());
        intent.putExtras(b);
        startActivity(intent);
    }

    public void openThx(View view) {
        startActivity(new Intent(MainActivity.this, CreditsActivity.class));
    }

    public void openStats(View view) {
        Intent intent = new Intent(MainActivity.this, StatsActivity.class);

        Bundle b = new Bundle();
        b.putString("game", this.game.getStats());
        intent.putExtras(b);
        startActivity(intent);
    }

    public void onDelete(View view) {

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.delete_dialog);

        Window window = dialog.getWindow();

        if (window != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView dialogBtn_cancel = dialog.findViewById(R.id.cancel);
        dialogBtn_cancel.setOnClickListener(v -> dialog.dismiss());

        TextView dialogBtn_okay = dialog.findViewById(R.id.confirm);
        dialogBtn_okay.setOnClickListener(v -> {
            MainActivity.this.game.deleteAll();
            Toast.makeText(getApplicationContext(), "Cleared all data.", Toast.LENGTH_SHORT).show();
            dialog.cancel();
        });

        dialog.show();
    }
}
