package com.satwant.focusguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TimerService extends Service {

    private static final String CHANNEL_ID = "focusguard_timer";
    private static final int NOTIF_ID = 1001;
    private CountDownTimer countDownTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long endTime = intent.getLongExtra("end_time", System.currentTimeMillis());
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) remaining = 1000;

        startForeground(NOTIF_ID, buildNotification("Focus session running…"));

        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(remaining, 30_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                updateNotification(minutes + " min remaining — stay focused");
            }

            @Override
            public void onFinish() {
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
                prefs.edit()
                        .putBoolean(MainActivity.KEY_SESSION_ACTIVE, false)
                        .putBoolean(MainActivity.KEY_COMMITMENT_MODE, false)
                        .apply();
                updateNotification("Focus session complete");
                stopForeground(false);
                stopSelf();
            }
        }.start();

        return START_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Focus Timer", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FocusGuard")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
