package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
    private Integer sisaHari;

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

    public Perkara getPerkara() {
        return perkara;
    }

    /**
     * Menghitung sisa hari sebelum deadline secara mandiri dari tanggal deadline
     * agar selalu akurat meskipun backend tidak menyertakan atribut sisa_hari di JSON.
     */
    public int getSisaHari() {
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

        return sisaHari != null ? sisaHari : 0;
    }

    public String getTersangkaNames() {
        if (perkara != null && perkara.getTersangkas() != null && !perkara.getTersangkas().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < perkara.getTersangkas().size(); i++) {
                sb.append(perkara.getTersangkas().get(i).getNama());
                if (i < perkara.getTersangkas().size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
        return "-";
    }

    /**
     * Memeriksa apakah suatu perkara berada pada Tahap 2 / Tahap II yang SEDANG BERJALAN.
     * Perkara yang sudah selesai (misal: "Tahap II Selesai") akan diabaikan (return false).
     */
    public static boolean isTahap2Active(Perkara perkara) {
        if (perkara == null) return false;
        String tahap = perkara.getTahapSaatIni() != null ? perkara.getTahapSaatIni().toLowerCase().trim() : "";
        String status = perkara.getStatusSaatIni() != null ? perkara.getStatusSaatIni().toLowerCase().trim() : "";

        // Abaikan perkara yang sudah selesai / dihentikan
        if (status.contains("selesai") || tahap.contains("selesai")) {
            return false;
        }

        // Periksa apakah status atau tahap mengindikasikan Tahap 2 yang sedang berjalan
        return tahap.contains("tahap ii") || tahap.contains("tahap 2")
                || status.contains("tahap ii") || status.contains("tahap 2")
                || status.contains("terima_tahap2");
    }

    /**
     * Membuat objek Reminder dari Perkara yang berada pada Tahap 2 / Tahap II yang sedang aktif
     * agar dapat ditampilkan di Menu Reminder SLA secara terpadu.
     */
    public static Reminder fromPerkaraTahap2(Perkara perkara) {
        if (perkara == null || !isTahap2Active(perkara)) return null;
        Reminder r = new Reminder();
        r.id = 900000 + perkara.getId();
        r.perkaraId = perkara.getId();
        r.perkara = perkara;
        r.tahap = (perkara.getTahapSaatIni() != null && !perkara.getTahapSaatIni().trim().isEmpty())
                ? perkara.getTahapSaatIni().trim() : "Tahap II";
        r.jenisReminder = (perkara.getStatusSaatIni() != null && !perkara.getStatusSaatIni().trim().isEmpty())
                ? perkara.getStatusSaatIni().trim() : "Pelaksanaan Tahap II";
        r.statusUrgensi = "Aman";
        r.isActive = true;
        r.sisaHari = 0;
        return r;
    }
}