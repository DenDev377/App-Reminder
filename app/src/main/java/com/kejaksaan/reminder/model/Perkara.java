package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Satu record Perkara (kasus) dari backend.
 * Sesuaikan field tambahan dengan struktur tabel Laravel Anda.
 */
public class Perkara {

    @SerializedName("id")
    private int id;

    @SerializedName("nomor_perkara")
    private String nomorPerkara;

    @SerializedName("tahap_saat_ini")
    private String tahapSaatIni;

    @SerializedName("status_saat_ini")
    private String statusSaatIni;

    @SerializedName("tersangkas")
    private List<Tersangka> tersangkas;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getId() { return id; }

    public String getNomorPerkara() { return nomorPerkara; }

    public String getTahapSaatIni() { return tahapSaatIni; }

    public String getStatusSaatIni() { return statusSaatIni; }

    public List<Tersangka> getTersangkas() { return tersangkas; }
}
