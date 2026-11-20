package com.kejaksaan.reminder.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.kejaksaan.reminder.DashboardActivity;
import com.kejaksaan.reminder.DetailPerkaraActivity;
import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.model.Reminder;
import com.kejaksaan.reminder.util.LocalAlarmScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AppWidgetProvider untuk Home Screen Widget (4x2):
 * - Menampilkan ringkasan perkara mendesak di layar depan ponsel Jaksa.
 * - Otomatis terbarui setiap ada sinkronisasi reminder dan alarm.
 * - Tombol pintas langsung ke Detail Perkara dan Dashboard.
 */
public class ReminderAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "ReminderWidget";
    public static final String ACTION_WIDGET_REFRESH = "com.kejaksaan.reminder.widget.ACTION_WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (appWidgetIds == null)
            return;
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null)
            return;

        if (ACTION_WIDGET_REFRESH.equals(intent.getAction())) {
            Log.d(TAG, "Tombol segarkan widget ditekan.");
            updateAllWidgets(context);
        }
    }

    public static void updateAllWidgets(Context context) {
        if (context == null)
            return;
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, ReminderAppWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Gagal memperbarui semua widget: " + e.getMessage());
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (context == null || appWidgetManager == null) return;
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_reminder);

            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            // 1. Klik Header / Container membuka DashboardActivity
            Intent dashboardIntent = new Intent(context, DashboardActivity.class);
            PendingIntent piDashboard = PendingIntent.getActivity(context, 100, dashboardIntent, pendingFlags);
            views.setOnClickPendingIntent(R.id.widgetRootContainer, piDashboard);

            // 2. Klik tombol refresh pada widget
            Intent refreshIntent = new Intent(context, ReminderAppWidgetProvider.class);
            refreshIntent.setAction(ACTION_WIDGET_REFRESH);
            PendingIntent piRefresh = PendingIntent.getBroadcast(context, 101, refreshIntent, pendingFlags);
            views.setOnClickPendingIntent(R.id.btnWidgetRefresh, piRefresh);

            // 3. Baca data pengingat dari cache lokal
            List<Reminder> cachedList = getFilteredActiveReminders(context);

            if (cachedList.isEmpty()) {
                views.setTextViewText(R.id.tvWidgetSummaryStatus, "Seluruh batas waktu perkara terkendali");
                views.setViewVisibility(R.id.layoutWidgetItem1, View.GONE);
                views.setViewVisibility(R.id.layoutWidgetItem2, View.GONE);
                views.setViewVisibility(R.id.layoutWidgetEmpty, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.layoutWidgetEmpty, View.GONE);

                // Hitung jumlah perkara yang mendesak (Overdue, H-0, atau H-3)
                int urgentCount = 0;
                for (Reminder r : cachedList) {
                    if (r != null && r.hasDeadline() && r.getSisaHari() <= 3) {
                        urgentCount++;
                    }
                }

                if (urgentCount > 0) {
                    views.setTextViewText(R.id.tvWidgetSummaryStatus, "⚠️ " + urgentCount + " Perkara Butuh Tindakan");
                } else {
                    views.setTextViewText(R.id.tvWidgetSummaryStatus, cachedList.size() + " Perkara Aktif Dipantau");
                }

                // Bind Slot 1
                Reminder r1 = cachedList.get(0);
                bindWidgetItem(context, views, r1, R.id.layoutWidgetItem1, R.id.tvWidgetDot1, R.id.tvWidgetNomor1,
                        R.id.tvWidgetTahap1, R.id.tvWidgetSisa1, 102);

                // Bind Slot 2 (jika ada)
                if (cachedList.size() > 1) {
                    Reminder r2 = cachedList.get(1);
                    views.setViewVisibility(R.id.layoutWidgetItem2, View.VISIBLE);
                    bindWidgetItem(context, views, r2, R.id.layoutWidgetItem2, R.id.tvWidgetDot2, R.id.tvWidgetNomor2,
                            R.id.tvWidgetTahap2, R.id.tvWidgetSisa2, 103);
                } else {
                    views.setViewVisibility(R.id.layoutWidgetItem2, View.GONE);
                }
            }

            // Timestamp Footer
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            views.setTextViewText(R.id.tvWidgetFooter,
                    "Diperbarui: " + sdf.format(new Date()) + " WIB • Sentuh untuk buka");

            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Gagal mengupdate widget #" + appWidgetId + ": " + e.getMessage(), e);
        }
    }

    private static void bindWidgetItem(Context context, RemoteViews views, Reminder reminder,
            int layoutId, int dotId, int nomorId, int tahapId, int sisaId, int reqCode) {
        views.setViewVisibility(layoutId, View.VISIBLE);

        int perkaraId = reminder.getPerkaraId();
        if (perkaraId <= 0 && reminder.getPerkara() != null) {
            perkaraId = reminder.getPerkara().getId();
        }

        String nomorPerkara = (reminder.getPerkara() != null && reminder.getPerkara().getNomorPerkara() != null)
                ? reminder.getPerkara().getNomorPerkara()
                : "Perkara #" + perkaraId;

        String tahap = reminder.getTahap() != null ? reminder.getTahap() : "Perkara";

        views.setTextViewText(nomorId, nomorPerkara);
        views.setTextViewText(tahapId, "Tahap: " + tahap);

        // Styling status berdasarkan urgensi / monitoring
        if (!reminder.hasDeadline() || reminder.isMonitoringStage()) {
            views.setTextViewText(dotId, "🔵");
            views.setTextViewText(sisaId, "Standby Tahap II");
            views.setTextColor(sisaId, Color.parseColor("#0284C7"));
        } else {
            int sisa = reminder.getSisaHari();
            if (sisa < 0) {
                views.setTextViewText(dotId, "🔴");
                views.setTextViewText(sisaId, "Lewat " + Math.abs(sisa) + " Hari");
                views.setTextColor(sisaId, Color.parseColor("#DC2626"));
            } else if (sisa == 0) {
                views.setTextViewText(dotId, "🔴");
                views.setTextViewText(sisaId, "Jatuh Tempo Hari Ini");
                views.setTextColor(sisaId, Color.parseColor("#DC2626"));
            } else if (sisa <= 3) {
                views.setTextViewText(dotId, "🟡");
                views.setTextViewText(sisaId, "Sisa " + sisa + " Hari");
                views.setTextColor(sisaId, Color.parseColor("#D97706"));
            } else {
                views.setTextViewText(dotId, "🟢");
                views.setTextViewText(sisaId, "Sisa " + sisa + " Hari");
                views.setTextColor(sisaId, Color.parseColor("#059669"));
            }
        }

        // Klik pada item membuka DetailPerkaraActivity perkara tersebut
        if (perkaraId > 0) {
            Intent detailIntent = new Intent(context, DetailPerkaraActivity.class);
            detailIntent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, perkaraId);
            detailIntent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, nomorPerkara);

            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent piDetail = PendingIntent.getActivity(context, reqCode, detailIntent, pendingFlags);
            views.setOnClickPendingIntent(layoutId, piDetail);
        }
    }

    private static List<Reminder> getFilteredActiveReminders(Context context) {
        List<Reminder> list = new ArrayList<>();
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("sipeta_local_alarms",
                    Context.MODE_PRIVATE);
            String json = prefs.getString("cached_reminders_json", null);
            if (json != null && !json.trim().isEmpty()) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Reminder>>() {
                }.getType();
                List<Reminder> parsed = gson.fromJson(json, listType);
                if (parsed != null) {
                    for (Reminder r : parsed) {
                        if (r != null && r.isActive()) {
                            list.add(r);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Gagal memuat cache untuk widget: " + e.getMessage());
        }

        // Urutkan: Overdue dan H-3 paling atas
        Collections.sort(list, (r1, r2) -> {
            int p1 = getWidgetPriority(r1);
            int p2 = getWidgetPriority(r2);
            if (p1 != p2)
                return Integer.compare(p1, p2);
            return Integer.compare(r1.getSisaHari(), r2.getSisaHari());
        });

        return list;
    }

    private static int getWidgetPriority(Reminder r) {
        if (r == null)
            return 99;
        if (!r.hasDeadline() || r.isMonitoringStage())
            return 7;
        int sisa = r.getSisaHari();
        if (sisa < 0)
            return 1;
        if (sisa == 0)
            return 2;
        if (sisa <= 3)
            return 3;
        return 5;
    }
}
