package com.kejaksaan.reminder.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

/**
 * Helper untuk mengelola Notification Channel pada Android 8.0 (API 26) ke atas.
 * Menggunakan CHANNEL_ID 'sipeta_reminder_channel' sesuai konfigurasi backend Laravel.
 */
public class NotificationHelper {

    public static final String CHANNEL_ID = "sipeta_reminder_channel";
    public static final String CHANNEL_NAME = "Notifikasi Pengingat Perkara";
    public static final String CHANNEL_DESC = "Peringatan SLA dan pembaruan tahapan perkara SIPETA Kejaksaan";

    /**
     * Daftarkan Notification Channel ke sistem Android jika perangkat menjalankan Android O ke atas.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription(CHANNEL_DESC);
                    channel.enableLights(true);
                    channel.setLightColor(Color.parseColor("#00897B"));
                    channel.enableVibration(true);
                    channel.setShowBadge(true);

                    notificationManager.createNotificationChannel(channel);
                }
            }
        }
    }
}
