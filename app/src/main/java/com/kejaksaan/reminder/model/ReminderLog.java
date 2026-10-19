package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Model entitas riwayat / audit trail Reminder SLA perkara.
 * Bersifat read-only untuk kebutuhan audit dan monitoring.
 */
public class ReminderLog {

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
    private Integer sisaHari;

    @SerializedName("status_log")
    private String statusLog;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

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

    public String getStatusLog() {
        return statusLog;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public Perkara getPerkara() {
        return perkara;
    }

    /**
     * Mengembalikan sisa hari dari JSON atau dihitung dari tanggal deadline jika null.
     */
    public int getSisaHari() {
        if (sisaHari != null) {
            return sisaHari;
        }

        if (deadline != null && !deadline.trim().isEmpty()) {
            try {
                String datePart = deadline.trim();
                if (datePart.contains("T")) {
                    datePart = datePart.substring(0, datePart.indexOf("T")).trim();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date targetDate = sdf.parse(datePart);
                if (targetDate != null) {
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    Calendar target = Calendar.getInstance();
                    target.setTime(targetDate);
                    target.set(Calendar.HOUR_OF_DAY, 0);
                    target.set(Calendar.MINUTE, 0);
                    target.set(Calendar.SECOND, 0);
                    target.set(Calendar.MILLISECOND, 0);

                    long diffMillis = target.getTimeInMillis() - today.getTimeInMillis();
                    return (int) Math.round((double) diffMillis / (24 * 60 * 60 * 1000));
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    public String getNomorPerkara() {
        if (perkara != null && perkara.getNomorPerkara() != null && !perkara.getNomorPerkara().isEmpty()) {
            return perkara.getNomorPerkara();
        }
        return "Perkara #" + perkaraId;
    }

    public String getTersangkaNames() {
        if (perkara != null) {
            return perkara.getTersangkaNames();
        }
        return "-";
    }

    public String getJaksaName() {
        if (perkara != null && perkara.getJaksa() != null && !perkara.getJaksa().trim().isEmpty()) {
            return perkara.getJaksa().trim();
        }
        return "-";
    }

    public String getPenyidik() {
        if (perkara != null && perkara.getPenyidik() != null && !perkara.getPenyidik().trim().isEmpty()) {
            return perkara.getPenyidik().trim();
        }
        return "-";
    }

    public String getStatusSaatIni() {
        if (perkara != null && perkara.getStatusSaatIni() != null && !perkara.getStatusSaatIni().trim().isEmpty()) {
            return perkara.getStatusSaatIni().trim();
        }
        return "-";
    }

    public String getTahapSaatIni() {
        if (perkara != null && perkara.getTahapSaatIni() != null && !perkara.getTahapSaatIni().trim().isEmpty()) {
            return perkara.getTahapSaatIni().trim();
        }
        return tahap != null ? tahap : "-";
    }

    public static String formatDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return "-";
        try {
            String datePart = dateString.trim();
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
        return dateString;
    }

    public static String formatDateTime(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return "-";
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US);
            sdfInput.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = sdfInput.parse(dateString);
            if (date == null) {
                sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                date = sdfInput.parse(dateString);
            }
            if (date != null) {
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", new Locale("id", "ID"));
                return sdfOutput.format(date);
            }
        } catch (Exception ignored) {
        }
        return formatDate(dateString);
    }
}
