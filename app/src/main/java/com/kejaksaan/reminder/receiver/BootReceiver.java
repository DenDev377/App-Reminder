package com.kejaksaan.reminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kejaksaan.reminder.util.LocalAlarmScheduler;

/**
 * Receiver yang terpanggil saat perangkat Android selesai booting (restart)
 * atau saat paket aplikasi diperbarui, untuk menjadwalkan ulang seluruh alarm offline.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        Log.i(TAG, "Menerima aksi sistem: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            LocalAlarmScheduler.rescheduleAllAlarms(context);
        }
    }
}

