package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model ringkasan metrik status reminder SLA pada audit log.
 */
public class ReminderLogSummary {

    @SerializedName("total_logs")
    private int totalLogs;

    @SerializedName("total_selesai")
    private int totalSelesai;

    @SerializedName("total_aktif")
    private int totalAktif;

    @SerializedName("total_overdue")
    private int totalOverdue;

    public int getTotalLogs() {
        return totalLogs;
    }

    public int getTotalSelesai() {
        return totalSelesai;
    }

    public int getTotalAktif() {
        return totalAktif;
    }

    public int getTotalOverdue() {
        return totalOverdue;
    }
}
