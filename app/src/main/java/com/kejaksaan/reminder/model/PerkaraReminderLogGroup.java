package com.kejaksaan.reminder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representasi agregasi 1 Card per Perkara pada menu Log Reminders.
 * Mengelompokkan seluruh riwayat tahapan reminder SLA untuk perkara/SPDP yang sama
 * agar tidak terjadi duplikasi card pada nomor perkara yang sama.
 */
public class PerkaraReminderLogGroup {

    private final int perkaraId;
    private final String nomorPerkara;
    private Perkara perkara;
    private final List<ReminderLog> reminderLogs = new ArrayList<>();
    private boolean isExpanded = false;

    public PerkaraReminderLogGroup(int perkaraId, String nomorPerkara, Perkara perkara) {
        this.perkaraId = perkaraId;
        this.nomorPerkara = nomorPerkara;
        this.perkara = perkara;
    }

    public int getPerkaraId() {
        return perkaraId;
    }

    public String getNomorPerkara() {
        if (nomorPerkara != null && !nomorPerkara.trim().isEmpty()) {
            return nomorPerkara.trim();
        }
        if (perkara != null && perkara.getNomorPerkara() != null && !perkara.getNomorPerkara().trim().isEmpty()) {
            return perkara.getNomorPerkara().trim();
        }
        return "Perkara #" + perkaraId;
    }

    public Perkara getPerkara() {
        return perkara;
    }

    public void setPerkara(Perkara perkara) {
        if (perkara != null) {
            this.perkara = perkara;
        }
    }

    public List<ReminderLog> getReminderLogs() {
        return reminderLogs;
    }

    /**
     * Menambahkan log reminder ke dalam grup perkara tanpa menduplikasi ID log.
     */
    public void addReminderLog(ReminderLog log) {
        if (log == null) return;
        for (ReminderLog existing : reminderLogs) {
            if (existing.getId() == log.getId()) {
                return;
            }
        }
        reminderLogs.add(log);
        sortLogs();
    }

    private void sortLogs() {
        // Urutkan tahapan berdasarkan tanggal deadline atau ID (terbaru di atas)
        Collections.sort(reminderLogs, (a, b) -> Integer.compare(b.getId(), a.getId()));
    }

    public int getTotalStages() {
        return reminderLogs.size();
    }

    public ReminderLog getLatestLog() {
        return !reminderLogs.isEmpty() ? reminderLogs.get(0) : null;
    }

    /**
     * Memeriksa apakah perkara ini sudah BENAR-BENAR SELESAI penanganannya
     * (yaitu: Pelaksanaan Tahap II Selesai, atau diterbitkan SOP FORM-7 / FORM-3 / P-20 / Dihentikan).
     * Perkara yang masih dalam tahap menunggu pelaksanaan Tahap II, P-24, P-16, dll adalah AKTIF.
     */
    public boolean isPerkaraFinished() {
        String t = getTahapSaatIni().toLowerCase();
        String s = getStatusSaatIni().toLowerCase();
        if (perkara != null) {
            if (perkara.getTahapSaatIni() != null) t += " " + perkara.getTahapSaatIni().toLowerCase();
            if (perkara.getStatusSaatIni() != null) s += " " + perkara.getStatusSaatIni().toLowerCase();
        }

        // Jika masih berstatus menunggu pelaksanaan Tahap II, menunggu penelitian berkas, atau belum selesai,
        // maka dipastikan perkara tersebut BELUM SELESAI (masih aktif berjalan)
        if (t.contains("menunggu") || s.contains("menunggu")
                || t.contains("belum selesai") || s.contains("belum selesai")) {
            return false;
        }

        // 1. Penghentian perkara / pengembalian SPDP secara resmi via SOP
        if (t.contains("form-7") || s.contains("form-7") || t.contains("form7") || s.contains("form7")
                || t.contains("form-3") || s.contains("form-3") || t.contains("form3") || s.contains("form3")
                || t.contains("p-20") || s.contains("p-20") || t.contains("p20") || s.contains("p20")
                || t.contains("dihentikan") || s.contains("dihentikan") || s.contains("sp3")) {
            return true;
        }

        // 2. Tahap II telah selesai dilaksanakan (eksekusi penyerahan tsk & bb telah tuntas)
        if (t.contains("tahap ii selesai") || t.contains("tahap 2 selesai")
                || s.contains("tahap ii selesai") || s.contains("tahap 2 selesai")
                || t.contains("selesai tahap ii") || s.contains("selesai tahap ii")
                || t.contains("selesai tahap 2") || s.contains("selesai tahap 2")) {
            return true;
        }

        // 3. Status selesai umum (pastikan bukan sekadar selesai tahap 1)
        if (!t.contains("tahap 1") && !t.contains("tahap i") && !s.contains("tahap 1") && !s.contains("tahap i")) {
            if (s.contains("selesai") || t.contains("selesai")) {
                return true;
            }
        }

        return false;
    }

    public boolean hasOverdue() {
        if (isPerkaraFinished()) return false;
        for (ReminderLog log : reminderLogs) {
            if (log.isActive()) {
                if (log.getStatusUrgensi() != null && log.getStatusUrgensi().equalsIgnoreCase("Overdue")) {
                    return true;
                }
                if (log.getStatusLog() != null && log.getStatusLog().toLowerCase().contains("overdue")) {
                    return true;
                }
                if (log.getSisaHari() < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasActive() {
        if (isPerkaraFinished()) return false;
        return true;
    }

    public String getPrimaryStatusBadge() {
        if (isPerkaraFinished()) {
            return "Selesai";
        }
        if (hasOverdue()) {
            return "Overdue";
        }
        return "Aktif";
    }

    public String getTersangkaNames() {
        if (perkara != null && perkara.getTersangkas() != null && !perkara.getTersangkas().isEmpty()) {
            return perkara.getTersangkaNames();
        }
        if (!reminderLogs.isEmpty()) {
            return reminderLogs.get(0).getTersangkaNames();
        }
        return "-";
    }

    public String getJaksaName() {
        if (perkara != null && perkara.getJaksa() != null && !perkara.getJaksa().trim().isEmpty()) {
            return perkara.getJaksa().trim();
        }
        if (!reminderLogs.isEmpty()) {
            return reminderLogs.get(0).getJaksaName();
        }
        return "-";
    }

    public String getPenyidik() {
        if (perkara != null && perkara.getPenyidik() != null && !perkara.getPenyidik().trim().isEmpty()) {
            return perkara.getPenyidik().trim();
        }
        if (!reminderLogs.isEmpty()) {
            return reminderLogs.get(0).getPenyidik();
        }
        return "-";
    }

    public String getTahapSaatIni() {
        if (perkara != null && perkara.getTahapSaatIni() != null && !perkara.getTahapSaatIni().trim().isEmpty()) {
            return perkara.getTahapSaatIni().trim();
        }
        if (!reminderLogs.isEmpty()) {
            return reminderLogs.get(0).getTahapSaatIni();
        }
        return "-";
    }

    public String getStatusSaatIni() {
        if (perkara != null && perkara.getStatusSaatIni() != null && !perkara.getStatusSaatIni().trim().isEmpty()) {
            return perkara.getStatusSaatIni().trim();
        }
        if (!reminderLogs.isEmpty()) {
            return reminderLogs.get(0).getStatusSaatIni();
        }
        return "-";
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
