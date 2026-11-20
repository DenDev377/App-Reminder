package com.kejaksaan.reminder.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.kejaksaan.reminder.util.LocalAlarmScheduler;
import com.kejaksaan.reminder.util.NotificationHelper;

/**
 * BroadcastReceiver untuk menangani tindakan interaktif notifikasi:
 * 1. ACTION_SNOOZE: Menunda pengingat perkara (default: 2 jam) dan mematikan notifikasi saat ini.
 * 2. ACTION_SHOW_SNOOZED_REMINDER: Memunculkan kembali notifikasi pengingat setelah masa tunda berakhir.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    private static final String TAG = "NotifActionReceiver";

    public static final String ACTION_SNOOZE = "com.kejaksaan.reminder.ACTION_SNOOZE";
    public static final String ACTION_SHOW_SNOOZED_REMINDER = "com.kejaksaan.reminder.ACTION_SHOW_SNOOZED_REMINDER";

    public static final String EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID";
    public static final String EXTRA_PERKARA_ID = "EXTRA_PERKARA_ID";
    public static final String EXTRA_NOMOR_PERKARA = "EXTRA_NOMOR_PERKARA";
    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_BODY = "EXTRA_BODY";
    public static final String EXTRA_SNOOZE_HOURS = "EXTRA_SNOOZE_HOURS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        int perkaraId = intent.getIntExtra(EXTRA_PERKARA_ID, -1);
        String nomorPerkara = intent.getStringExtra(EXTRA_NOMOR_PERKARA);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);

        if (ACTION_SNOOZE.equals(action)) {
            handleSnooze(context, intent, notificationId, perkaraId, nomorPerkara, title, body);
        } else if (ACTION_SHOW_SNOOZED_REMINDER.equals(action)
                || LocalAlarmScheduler.ACTION_SHOW_SCHEDULED_ALARM.equals(action)) {
            handleShowSnoozedReminder(context, notificationId, perkaraId, nomorPerkara, title, body);
        }
    }

    private void handleSnooze(Context context, Intent intent, int notificationId, int perkaraId,
                              String nomorPerkara, String title, String body) {
        // 1. Batalkan notifikasi yang sedang aktif di bilah status
        if (notificationId > 0) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        int snoozeHours = intent.getIntExtra(EXTRA_SNOOZE_HOURS, 2);
        long snoozeMillis = snoozeHours * 60 * 60 * 1000L;
        long triggerAtMillis = System.currentTimeMillis() + snoozeMillis;

        // 2. Berikan feedback ke pengguna
        String toastMsg = "Pengingat perkara ditunda " + snoozeHours + " jam";
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Notifikasi ID " + notificationId + " ditunda selama " + snoozeHours + " jam.");

        // 3. Jadwalkan alarm lokal untuk memunculkan notifikasi kembali
        Intent alarmIntent = new Intent(context, NotificationActionReceiver.class);
        alarmIntent.setAction(ACTION_SHOW_SNOOZED_REMINDER);
        alarmIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        alarmIntent.putExtra(EXTRA_PERKARA_ID, perkaraId);
        alarmIntent.putExtra(EXTRA_NOMOR_PERKARA, nomorPerkara);
        alarmIntent.putExtra(EXTRA_TITLE, title);
        alarmIntent.putExtra(EXTRA_BODY, body);

        int requestCode = (notificationId > 0) ? notificationId + 50000 : (int) (System.currentTimeMillis() % 100000);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, alarmIntent, pendingFlags);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }

    private void handleShowSnoozedReminder(Context context, int notificationId, int perkaraId,
                                           String nomorPerkara, String title, String body) {
        Log.i(TAG, "Membangunkan pengingat yang ditunda untuk perkara ID: " + perkaraId);

        String alertTitle = (title != null && !title.isEmpty()) ? title : "Pengingat Lanjutan SIPETA";
        String alertBody = (body != null && !body.isEmpty()) ? body : "Pengingat batas waktu perkara kembali aktif.";

        // Tampilkan kembali notifikasi menggunakan NotificationHelper terpusat
        NotificationHelper.showReminderNotification(context, notificationId, perkaraId, nomorPerkara, alertTitle, alertBody);
    }
}

