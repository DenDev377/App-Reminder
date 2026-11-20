package com.kejaksaan.reminder.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.kejaksaan.reminder.DetailPerkaraActivity;
import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.receiver.NotificationActionReceiver;

/**
 * Helper untuk mengelola Notification Channel dan menampilkan notifikasi pengingat perkara
 * dengan nada dering kustom resmi Kejaksaan dan tombol interaktif (Buka Perkara & Tunda 2 Jam).
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    // Channel V2 agar Android OS memperbarui pengaturan suara kustom dan pola getar
    public static final String CHANNEL_ID = "sipeta_reminder_channel_v2";
    public static final String CHANNEL_NAME = "Pengingat Batas Waktu Perkara";
    public static final String CHANNEL_DESC = "Peringatan SLA, masa tenggat berkas, dan jadwal perkara SIPETA Kejaksaan";

    public static final String CHANNEL_GENERAL_ID = "sipeta_general_channel";
    public static final String CHANNEL_GENERAL_NAME = "Informasi & Berita Umum";
    public static final String CHANNEL_GENERAL_DESC = "Pembaruan informasi kedinasan dan berita Kejaksaan";

    /**
     * Mendapatkan URI nada dering kustom resmi Kejaksaan dari res/raw/kejaksaan_bell.wav.
     */
    public static Uri getCustomSoundUri(Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.kejaksaan_bell);
    }

    /**
     * Daftarkan Notification Channel ke sistem Android jika perangkat menjalankan Android O (API 26) ke atas.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // 1. Saluran Prioritas Tinggi (Pengingat Perkara & SLA) dengan suara kustom & pola getar
                NotificationChannel existingReminderChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingReminderChannel == null) {
                    NotificationChannel reminderChannel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    reminderChannel.setDescription(CHANNEL_DESC);
                    reminderChannel.enableLights(true);
                    reminderChannel.setLightColor(Color.parseColor("#00897B"));
                    reminderChannel.enableVibration(true);
                    reminderChannel.setVibrationPattern(new long[]{0, 400, 200, 400, 200, 400});
                    reminderChannel.setShowBadge(true);

                    Uri soundUri = getCustomSoundUri(context);
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build();
                    reminderChannel.setSound(soundUri, audioAttributes);

                    notificationManager.createNotificationChannel(reminderChannel);
                }

                // 2. Saluran Informasi Umum
                NotificationChannel existingGeneralChannel = notificationManager.getNotificationChannel(CHANNEL_GENERAL_ID);
                if (existingGeneralChannel == null) {
                    NotificationChannel generalChannel = new NotificationChannel(
                            CHANNEL_GENERAL_ID,
                            CHANNEL_GENERAL_NAME,
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
                    generalChannel.setDescription(CHANNEL_GENERAL_DESC);
                    generalChannel.enableLights(true);
                    generalChannel.setLightColor(Color.parseColor("#00897B"));
                    notificationManager.createNotificationChannel(generalChannel);
                }
            }
        }
    }

    /**
     * Membangun dan menampilkan notifikasi pengingat perkara lengkap dengan tombol aksi cepat (Buka & Tunda).
     */
    public static void showReminderNotification(Context context, int notificationId, int perkaraId,
                                                String nomorPerkara, String title, String body) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, DetailPerkaraActivity.class);
        intent.setAction("OPEN_PERKARA_DETAIL_" + (notificationId > 0 ? notificationId : System.currentTimeMillis()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (perkaraId > 0) {
            intent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, perkaraId);
            intent.putExtra("perkara_id", String.valueOf(perkaraId));
        }
        if (nomorPerkara != null && !nomorPerkara.trim().isEmpty()) {
            intent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, nomorPerkara.trim());
            intent.putExtra("nomor_perkara", nomorPerkara.trim());
        }

        int requestCode = (notificationId > 0) ? notificationId : (int) (System.currentTimeMillis() % 100000);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, requestCode, intent, pendingFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.parseColor("#00897B"))
                .setAutoCancel(true)
                .setSound(getCustomSoundUri(context))
                .setVibrate(new long[]{0, 400, 200, 400, 200, 400})
                .setContentIntent(contentPendingIntent);

        // Tambahkan tombol aksi interaktif jika ada perkara yang terkait
        if (perkaraId > 0) {
            // Tombol 1: Buka Perkara
            Intent openIntent = new Intent(context, DetailPerkaraActivity.class);
            openIntent.setAction("ACTION_OPEN_" + perkaraId);
            openIntent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, perkaraId);
            if (nomorPerkara != null) {
                openIntent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, nomorPerkara);
            }
            PendingIntent openPendingIntent = PendingIntent.getActivity(context, requestCode + 1, openIntent, pendingFlags);
            builder.addAction(R.drawable.ic_perkara, "Buka Perkara", openPendingIntent);

            // Tombol 2: Tunda 2 Jam (Snooze)
            Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
            snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
            snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId > 0 ? notificationId : requestCode);
            snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_PERKARA_ID, perkaraId);
            snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_NOMOR_PERKARA, nomorPerkara);
            snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_TITLE, title);
            snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_BODY, body);
            snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_SNOOZE_HOURS, 2);

            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, requestCode + 2, snoozeIntent, pendingFlags);
            builder.addAction(R.drawable.ic_alarm, "Tunda 2 Jam", snoozePendingIntent);
        }

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            int actualId = (notificationId > 0) ? notificationId : ((perkaraId > 0) ? perkaraId : requestCode);
            manager.notify(actualId, builder.build());
            Log.i(TAG, "Notifikasi pengingat perkara berhasil dikirim (ID: " + actualId + ")");
        } else {
            Log.w(TAG, "Izin POST_NOTIFICATIONS belum diberikan oleh pengguna.");
        }
    }
}
