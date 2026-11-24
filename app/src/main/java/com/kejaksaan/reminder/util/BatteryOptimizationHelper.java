package com.kejaksaan.reminder.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
 *
 * Menerapkan alur Session-Scoped Prompting:
 * - Jika pengguna memilih 'Nanti Saja', dialog tidak akan mengganggu lagi di sesi yang sama.
 * - Saat pengguna logout dan login kembali (sesi baru), dialog akan otomatis muncul kembali
 *   sampai pengecualian baterai benar-benar diizinkan.
 */
public class BatteryOptimizationHelper {

    private static final String TAG = "BatteryOptimization";

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
     * Menggunakan siklus per-sesi (Session-Scoped):
     * - Tidak memunculkan dialog berulang kali di sesi yang sama jika pengguna memilih 'Nanti Saja'.
     * - Mengingatkan kembali secara otomatis saat pengguna login ulang ke aplikasi.
     */
    public static void checkAndPromptBatteryOptimization(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        // Bersihkan data preferensi lama (3-day cooldown) jika masih tersisa
        try {
            activity.getSharedPreferences("battery_opt_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
        } catch (Exception ignored) {}

        // Jika sudah diizinkan bebas baterai, tidak perlu dialog
        if (isIgnoringBatteryOptimizations(activity)) {
            Log.d(TAG, "Aplikasi sudah dalam daftar putih bebas penghemat baterai.");
            return;
        }

        SessionManager sessionManager = new SessionManager(activity);

        // Jangan mengganggu jika pengguna sudah memilih 'Nanti Saja' pada sesi login saat ini
        if (sessionManager.isBatteryOptDismissedInSession()) {
            Log.d(TAG, "Pengguna telah memilih Nanti Saja pada sesi ini. Lewati dialog.");
            return;
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Optimalisasi Alarm & Pengingat")
                .setMessage("Agar alarm batas waktu perkara (P-24, P-19, dan SLA) dapat berdering tepat waktu di ponsel Anda tanpa tertunda atau dibekukan sistem penghemat baterai, izinkan SIPETA berjalan di latar belakang tanpa pembatasan baterai.")
                .setPositiveButton("Bebaskan Pembatasan", (dialog, which) -> {
                    openBatteryOptimizationSettings(activity);
                })
                .setNegativeButton("Nanti Saja", (dialog, which) -> {
                    sessionManager.setBatteryOptDismissedInSession(true);
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> {
                    sessionManager.setBatteryOptDismissedInSession(true);
                })
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
