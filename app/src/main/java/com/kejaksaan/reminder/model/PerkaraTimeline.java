package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PerkaraTimeline {

    @SerializedName("id")
    private int id;

    @SerializedName("perkara_id")
    private int perkaraId;

    @SerializedName("tahap")
    private String tahap;

    @SerializedName("status_kegiatan")
    private String statusKegiatan;

    @SerializedName("tanggal_kejadian")
    private String tanggalKejadian;

    @SerializedName("catatan")
    private String catatan;

    @SerializedName("actor_type")
    private String actorType;

    @SerializedName("user")
    private User user;

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

    public String getStatusKegiatan() {
        return statusKegiatan;
    }

    public String getTanggalKejadian() {
        return tanggalKejadian;
    }

    public String getCatatan() {
        return catatan;
    }

    public String getActorType() {
        return actorType;
    }

    public User getUser() {
        return user;
    }

    public Perkara getPerkara() {
        return perkara;
    }

    public boolean isSystem() {
        return "SYSTEM".equalsIgnoreCase(actorType);
    }

    public String getActorDisplayName() {
        if (isSystem()) {
            return "Sistem (Otomatis)";
        }
        if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
            return user.getName();
        }
        return "Petugas";
    }

    public String getFormattedTanggal() {
        if (tanggalKejadian == null || tanggalKejadian.trim().isEmpty()) return "-";
        try {
            String datePart = tanggalKejadian.trim();
            if (datePart.contains("T")) {
                datePart = datePart.substring(0, datePart.indexOf("T")).trim();
            }
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfInput.parse(datePart);
            if (date != null) {
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
                return sdfOutput.format(date);
            }
        } catch (Exception ignored) {
        }
        return tanggalKejadian;
    }
}