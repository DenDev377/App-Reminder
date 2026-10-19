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

    public boolean hasOverdue() {
        for (ReminderLog log : reminderLogs) {
            if (log.getStatusUrgensi() != null && log.getStatusUrgensi().equalsIgnoreCase("Overdue")) {
                return true;
            }
            if (log.getStatusLog() != null && log.getStatusLog().toLowerCase().contains("overdue")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActive() {
        for (ReminderLog log : reminderLogs) {
            if (log.isActive()) {
                return true;
            }
            if (log.getStatusLog() != null && log.getStatusLog().equalsIgnoreCase("Aktif")) {
                return true;
            }
        }
        return false;
    }

    public String getPrimaryStatusBadge() {
        if (hasOverdue()) {
            return "Overdue";
        }
        if (hasActive()) {
            return "Aktif";
        }
        return "Selesai";
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
