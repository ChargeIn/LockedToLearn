package com.flop.lockedtolearn.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.flop.lockedtolearn.R;
import com.flop.lockedtolearn.game.Game;

public class LockscreenService extends Service {

    static final String NOTIFICATION_CHANNEL_ID = "com.flop.lockedtolearn";
    static final String CHANNEL_NAME = "Locked to Learn";

    public ScreenReceiver receiver = null;

    public int phoneState = TelephonyManager.CALL_STATE_IDLE;
    public boolean interrupted = false;
    private Game game;

    @Override
    public void onCreate() {
        super.onCreate();
        receiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(receiver, filter);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        this.game = new Game(this, vibrator);
        this.game.setOnSolve(this::unlockScreen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            class CallbackStateChange extends TelephonyCallback implements TelephonyCallback.CallStateListener {

                @Override
                public void onCallStateChanged(int state) {
                    callStateChanged(state, "");
                }
            }

            telephonyManager.registerTelephonyCallback(
                    getApplicationContext().getMainExecutor(),
                    new CallbackStateChange());
        } else {
            telephonyManager.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    callStateChanged(state, incomingNumber);
                    super.onCallStateChanged(state, incomingNumber);
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }
        startMyOwnForeground();
    }

    private void startMyOwnForeground() {
        NotificationChannel channel = new NotificationChannel(LockscreenService.NOTIFICATION_CHANNEL_ID, LockscreenService.CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSubText("I make sure you are learning :)")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(NotificationManager.IMPORTANCE_NONE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    public boolean isLocked() {
        // TODO
        return this.view != null;
    }

    public void lockScreen() {
        if (isLocked()) {
            // TODO: update here
            return;
        }

        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                point.y + 500,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        View lockView = this.game.createLockView();

        if (lockView == null) {
            this.unlockScreen();
            return;
        }

        Log.i("Service", "Test this text");
        System.out.println("Adding view");
        // TODO add view here
        windowManager.addView(lockView, layoutParams);
        this.view = lockView;
    }

    private View view;

    public void unlockScreen() {
        if (!isLocked()) {
            return;
        }
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.removeView(this.view);
        this.view = null;
    }

    public void callStateChanged(int state, String incomingNumber) {
        phoneState = state;
        if (state == TelephonyManager.CALL_STATE_IDLE) {
            if (interrupted) {
                lockScreen();
                interrupted = false;
            }
        } else if (state == TelephonyManager.CALL_STATE_RINGING) {
            if (isLocked()) {
                interrupted = true;
                unlockScreen();
            }
        }
    }

    public class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

                if (phoneState != TelephonyManager.CALL_STATE_IDLE) {
                    return;
                }
                game.prepareSet();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                lockScreen();
            }
        }
    }
}
