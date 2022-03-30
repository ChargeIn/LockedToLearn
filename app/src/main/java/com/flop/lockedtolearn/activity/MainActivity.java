package com.flop.lockedtolearn.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.flop.lockedtolearn.R;
import com.flop.lockedtolearn.game.Game;
import com.flop.lockedtolearn.service.LockscreenService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.w3c.dom.Text;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    boolean serviceRunning = false;

    TextView lockText;
    ImageView lockImage;
    TextView upload;
    TextView download;
    LinearLayout startCard;
    Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        game = new Game(getApplicationContext(), vibrator);

        serviceRunning = isServiceRunning();

        lockText = findViewById(R.id.lock);
        lockText.setText(serviceRunning ? "Stop Learning" : "Start Learning");

        startCard = findViewById(R.id.start_card);
        startCard.setBackground(ContextCompat.getDrawable(getApplicationContext(), (serviceRunning ? R.drawable.stop_card_bg : R.drawable.start_card_bg)));

        lockImage = findViewById(R.id.lock_img);
        lockImage.setBackground(ContextCompat.getDrawable(getApplicationContext(), (serviceRunning ? R.drawable.stop : R.drawable.start)));

        upload = findViewById(R.id.upload);
        download = findViewById(R.id.download_template);
    }

    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 2323;

    private void requestOverlayPermission() {
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
        startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
    }

    private void requestFileBrowserPermissions() {
        this.requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1001 // any number
        );
    }

    private boolean checkOverLayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private boolean checkFileBrowserPermission() {
        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LockscreenService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void onToggleLock(View view) {
        Intent intent = new Intent(MainActivity.this, LockscreenService.class);
        if (serviceRunning) {
            stopService(intent);
            serviceRunning = !serviceRunning;
        } else {
            if (!checkOverLayPermission()) {
                requestOverlayPermission();
            } else {
                startService(intent);
                serviceRunning = !serviceRunning;
            }
        }
        lockText.setText(serviceRunning ? "Stop Learning" : "Start Learning");
        startCard.setBackground(ContextCompat.getDrawable(getApplicationContext(), (serviceRunning ? R.drawable.stop_card_bg : R.drawable.start_card_bg)));
        lockImage.setBackground(ContextCompat.getDrawable(getApplicationContext(), (serviceRunning ? R.drawable.stop : R.drawable.start)));
    }

    ActivityResultLauncher<Intent> uploadARLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Uri uri = result.getData().getData();
                    upload.setText(R.string.loading);
                    new Thread(() -> {
                        try {
                            String message = game.addFile(uri);
                            runOnUiThread(() -> {
                                upload.setText(R.string.upload_btn);
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

                                // restart service to load new game context
                                Intent intent = new Intent(MainActivity.this, LockscreenService.class);

                                if (this.isServiceRunning()) {
                                    stopService(intent);
                                    startService(intent);
                                }
                            });
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                upload.setText(R.string.upload_btn);
                                Toast.makeText(getApplicationContext(), "Upload failed.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                }
            }
    );

    public void onUploadFile(View view) {
        if (game.isLoading()) {
            return;
        }

        if (!checkFileBrowserPermission()) {
            this.requestFileBrowserPermissions();
        }
        Intent file = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        file.setType("*/*");
        file = Intent.createChooser(file, "Choose a file");
        uploadARLauncher.launch(file);
    }

    ActivityResultLauncher<Intent> downloadARLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Uri uri = result.getData().getData();
                    download.setText(R.string.downloading);
                    new Thread(() -> {
                        try {
                            String message = game.writeTemplate(uri);
                            runOnUiThread(() -> {
                                download.setText(R.string.download_btn);
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                            });
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                Log.i(".---------", e.getMessage());
                                download.setText(R.string.download_btn);
                                Toast.makeText(getApplicationContext(), "Download failed.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                }
            }
    );

    public void onDownloadTemplate(View view) {
        if (game.isDownloading()) {
            return;
        }

        if (!checkFileBrowserPermission()) {
            this.requestFileBrowserPermissions();
        }
        Intent file = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        file.setType("*/*.xls");
        file.putExtra(Intent.EXTRA_TITLE, "template.xls");
        file = Intent.createChooser(file, "Select a file path (.xls).");
        downloadARLauncher.launch(file);
    }

    public void openHowTo(View view) {
        startActivity(new Intent(MainActivity.this, HowToActivity.class));
    }

    public void openSoon(View view) {
        startActivity(new Intent(MainActivity.this, SoonActivity.class));
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

    public void onDelete(View view){

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.delete_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView dialogBtn_cancel = dialog.findViewById(R.id.cancel);
        dialogBtn_cancel.setOnClickListener(v -> dialog.dismiss());

        TextView dialogBtn_okay = dialog.findViewById(R.id.confirm);
        dialogBtn_okay.setOnClickListener(v -> {
            MainActivity.this.game.deleteAll();
            Toast.makeText(getApplicationContext(),"Cleared all data." ,Toast.LENGTH_SHORT).show();
            dialog.cancel();
        });

        dialog.show();
    }
}
