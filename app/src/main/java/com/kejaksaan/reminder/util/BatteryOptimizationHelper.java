package com.kejaksaan.reminder.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Helper untuk mengelola pengecualian penghemat baterai (Battery Optimization Exemption).
 * Sangat penting pada ponsel vendor populer (Xiaomi, Samsung, Oppo, Vivo) agar alarm
 * mandiri dan background push tidak dibunuh/dibekukan oleh sistem OS.
 */
public class BatteryOptimizationHelper {

    private static final String TAG = "BatteryOptimization";
    private static final String PREF_NAME = "battery_opt_prefs";
    private static final String KEY_LAST_PROMPT = "last_prompt_timestamp";
    private static final long PROMPT_INTERVAL_MILLIS = 3L * 24 * 60 * 60 * 1000L; // 3 hari

    /**
     * Memeriksa apakah aplikasi sudah masuk daftar putih (whitelist) bebas penghemat baterai.
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (context == null) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    /**
     * Menampilkan dialog edukasi dan meminta izin pengecualian baterai jika belum diizinkan.
     * Tidak akan memunculkan dialog berulang kali jika pengguna memilih 'Nanti Saja' dalam 3 hari.
     */
    public static void checkAndPromptBatteryOptimization(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        // Jika sudah diizinkan bebas baterai, tidak perlu dialog
        if (isIgnoringBatteryOptimizations(activity)) {
            Log.d(TAG, "Aplikasi sudah dalam daftar putih bebas penghemat baterai.");
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0);
        long now = System.currentTimeMillis();

        // Jangan mengganggu pengguna jika baru ditolak dalam 3 hari terakhir
        if (now - lastPrompt < PROMPT_INTERVAL_MILLIS) {
            return;
        }

        // Catat waktu kemunculan dialog
        prefs.edit().putLong(KEY_LAST_PROMPT, now).apply();

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Optimalisasi Alarm & Pengingat")
                .setMessage("Agar alarm batas waktu perkara (P-24, P-19, dan SLA) dapat berdering tepat waktu di ponsel Anda tanpa tertunda atau dibekukan sistem penghemat baterai, izinkan SIPETA berjalan di latar belakang tanpa pembatasan baterai.")
                .setPositiveButton("Bebaskan Pembatasan", (dialog, which) -> {
                    openBatteryOptimizationSettings(activity);
                })
                .setNegativeButton("Nanti Saja", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    /**
     * Membuka layar pengaturan sistem untuk membebaskan pembatasan baterai bagi aplikasi SIPETA.
     */
    public static void openBatteryOptimizationSettings(Activity activity) {
        if (activity == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent fallbackIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    activity.startActivity(fallbackIntent);
                } catch (Exception ex) {
                    Toast.makeText(activity, "Buka Pengaturan HP > Aplikasi > SIPETA > Penggunaan Baterai > Pilih 'Tanpa Batasan'", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}

