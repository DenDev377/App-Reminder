package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

public class Reminder {

    @SerializedName("id")
    private int id;

    @SerializedName("perkara_id")
    private int perkaraId;

    @SerializedName("tahap")
    private String tahap;

    @SerializedName("jenis_reminder")
    private String jenisReminder;

    @SerializedName("tanggal_mulai")
    private String tanggalMulai;

    @SerializedName("deadline")
    private String deadline;

    @SerializedName("status_urgensi")
    private String statusUrgensi;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("sisa_hari")
    private int sisaHari;

    @SerializedName("perkara")
    private Perkara perkara;

    public int getId() {
        return id;
    }

    public int getPerkaraId() {
        return perkaraId;
    }

    public String getTahap() {
        return tahap;
    }

    public String getJenisReminder() {
        return jenisReminder;
    }

    public String getTanggalMulai() {
        return tanggalMulai;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getStatusUrgensi() {
        return statusUrgensi;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getSisaHari() {
        return sisaHari;
    }

    public Perkara getPerkara() {
        return perkara;
    }
}
