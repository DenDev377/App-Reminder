package com.kejaksaan.reminder.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kejaksaan.reminder.model.Reminder;
import com.kejaksaan.reminder.receiver.NotificationActionReceiver;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manajer penjadwalan alarm lokal offline (Local Alarm Scheduler):
 * - Menghitung tonggak batas waktu (H-3, H-1, H-0) pada pukul 08:00 WIB.
 * - Menjadwalkan alarm mandiri via AlarmManager yang berjalan 100% offline (tanpa sinyal/internet).
 * - Menyimpan riwayat pengingat aktif ke SharedPreferences untuk penjadwalan ulang saat HP restart.
 */
public class LocalAlarmScheduler {

    private static final String TAG = "LocalAlarmScheduler";
    private static final String PREF_NAME = "sipeta_local_alarms";
    private static final String KEY_CACHED_REMINDERS = "cached_reminders_json";

    public static final String ACTION_SHOW_SCHEDULED_ALARM = "com.kejaksaan.reminder.ACTION_SHOW_SCHEDULED_ALARM";

    // Target jam alarm berdering di pagi hari (pukul 08:00 pagi)
    private static final int ALARM_HOUR = 8;
    private static final int ALARM_MINUTE = 0;

    /**
     * Menjadwalkan alarm offline untuk daftar pengingat aktif yang memiliki deadline.
     */
    public static void scheduleAlarmsForReminders(Context context, List<Reminder> reminders) {
        if (context == null || reminders == null) return;

        // 1. Simpan daftar pengingat aktif ke penyimpanan lokal (cache)
        cacheRemindersLocally(context, reminders);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long now = System.currentTimeMillis();
        int scheduledCount = 0;

        for (Reminder reminder : reminders) {
            if (reminder == null || !reminder.hasDeadline() || !reminder.isActive()) {
                continue;
            }

            Date deadlineDate = parseDeadlineDate(reminder.getDeadline());
            if (deadlineDate == null) continue;

            int perkaraId = reminder.getPerkaraId();
            if (perkaraId <= 0 && reminder.getPerkara() != null) {
                perkaraId = reminder.getPerkara().getId();
            }
            if (perkaraId <= 0) continue;

            String nomorPerkara = (reminder.getPerkara() != null && reminder.getPerkara().getNomorPerkara() != null)
                    ? reminder.getPerkara().getNomorPerkara()
                    : "Perkara #" + perkaraId;

            String tersangka = reminder.getTersangkaNames();
            String tahap = reminder.getTahap() != null ? reminder.getTahap() : "Perkara";

            // Format tanggal deadline untuk body notifikasi
            SimpleDateFormat displaySdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
            String deadlineFormatted = displaySdf.format(deadlineDate);

            // Jadwalkan 3 Tonggak Batas Waktu: H-3, H-1, dan H-0
            int[] milestones = new int[]{3, 1, 0};
            for (int milestone : milestones) {
                long triggerAtMillis = calculateTriggerMillis(deadlineDate, milestone);

                // Hanya jadwalkan jika waktu alarm berada di masa depan
                if (triggerAtMillis > now) {
                    String title;
                    String body;

                    if (milestone == 3) {
                        title = "⚠️ Peringatan H-3: Batas Waktu " + tahap;
                        body = "Perkara " + nomorPerkara + " (" + tersangka + ") jatuh tempo pada " + deadlineFormatted + ".";
                    } else if (milestone == 1) {
                        title = "⏳ Peringatan H-1: Besok Jatuh Tempo " + tahap;
                        body = "Besok batas akhir penelitian/tindakan berkas perkara " + nomorPerkara + ".";
                    } else {
                        title = "🔴 HARI INI JATUH TEMPO: " + tahap;
                        body = "Hari ini tenggat waktu perkara " + nomorPerkara + " (" + tersangka + "). Segera selesaikan tindakan!";
                    }

                    int requestCode = generateRequestCode(perkaraId, milestone);
                    int notificationId = generateNotificationId(perkaraId, milestone);

                    Intent intent = new Intent(context, NotificationActionReceiver.class);
                    intent.setAction(ACTION_SHOW_SCHEDULED_ALARM);
                    intent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
                    intent.putExtra(NotificationActionReceiver.EXTRA_PERKARA_ID, perkaraId);
                    intent.putExtra(NotificationActionReceiver.EXTRA_NOMOR_PERKARA, nomorPerkara);
                    intent.putExtra(NotificationActionReceiver.EXTRA_TITLE, title);
                    intent.putExtra(NotificationActionReceiver.EXTRA_BODY, body);

                    int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
                    }

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, pendingFlags);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    }

                    scheduledCount++;
                }
            }
        }

        Log.i(TAG, "Berhasil menyelaraskan " + scheduledCount + " jadwal alarm offline mandiri.");
    }

    /**
     * Menjadwalkan ulang seluruh alarm dari data cache lokal (dipanggil oleh BootReceiver saat HP restart).
     */
    public static void rescheduleAllAlarms(Context context) {
        if (context == null) return;
        List<Reminder> cachedList = getCachedReminders(context);
        if (!cachedList.isEmpty()) {
            Log.i(TAG, "Menjadwalkan ulang alarm dari cache lokal sebanyak " + cachedList.size() + " data.");
            scheduleAlarmsForReminders(context, cachedList);
        }
    }

    /**
     * Membatalkan seluruh alarm untuk perkara yang sudah diselesaikan tindakannya.
     */
    public static void cancelAlarmsForPerkara(Context context, int perkaraId) {
        if (context == null || perkaraId <= 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int[] milestones = new int[]{3, 1, 0};
        for (int milestone : milestones) {
            int requestCode = generateRequestCode(perkaraId, milestone);
            Intent intent = new Intent(context, NotificationActionReceiver.class);
            intent.setAction(ACTION_SHOW_SCHEDULED_ALARM);

            int pendingFlags = PendingIntent.FLAG_NO_CREATE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, pendingFlags);
            if (pi != null) {
                alarmManager.cancel(pi);
                pi.cancel();
            }
        }

        Log.i(TAG, "Seluruh alarm lokal untuk perkara ID " + perkaraId + " telah dibatalkan.");
    }

    // ─── Private Helpers ────────────────────────────────────────────────────────

    private static long calculateTriggerMillis(Date deadlineDate, int daysBefore) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(deadlineDate);
        cal.add(Calendar.DAY_OF_YEAR, -daysBefore);
        cal.set(Calendar.HOUR_OF_DAY, ALARM_HOUR);
        cal.set(Calendar.MINUTE, ALARM_MINUTE);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static Date parseDeadlineDate(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) return null;
        try {
            String datePart = deadlineStr.trim();
            if (datePart.contains("T")) {
                datePart = datePart.substring(0, datePart.indexOf("T")).trim();
            } else if (datePart.contains(" ")) {
                datePart = datePart.substring(0, datePart.indexOf(" ")).trim();
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.parse(datePart);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int generateRequestCode(int perkaraId, int milestone) {
        return (perkaraId * 10) + milestone;
    }

    private static int generateNotificationId(int perkaraId, int milestone) {
        return 700000 + (perkaraId * 10) + milestone;
    }

    private static void cacheRemindersLocally(Context context, List<Reminder> reminders) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = gson.toJson(reminders);
            prefs.edit().putString(KEY_CACHED_REMINDERS, json).apply();

            // Perbarui tampilan Home Screen Widget secara real-time
            com.kejaksaan.reminder.widget.ReminderAppWidgetProvider.updateAllWidgets(context);
        } catch (Exception e) {
            Log.w(TAG, "Gagal menyimpan cache pengingat lokal: " + e.getMessage());
        }
    }

    public static List<Reminder> getCachedReminders(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_CACHED_REMINDERS, null);
            if (json != null && !json.trim().isEmpty()) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Reminder>>() {}.getType();
                List<Reminder> list = gson.fromJson(json, listType);
                if (list != null) return list;
            }
        } catch (Exception e) {
            Log.w(TAG, "Gagal memuat cache pengingat lokal: " + e.getMessage());
        }
        return new ArrayList<>();
    }
}

