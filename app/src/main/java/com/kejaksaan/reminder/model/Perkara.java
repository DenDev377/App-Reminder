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

    @SerializedName("penyidik")
    private String penyidik;

    @SerializedName("jaksa")
    private String jaksa;

    @SerializedName("tersangkas")
    private List<Tersangka> tersangkas;

    public int getId() { return id; }

    public String getNomorPerkara() { return nomorPerkara; }

    public String getTahapSaatIni() { return tahapSaatIni; }

    public String getStatusSaatIni() { return statusSaatIni; }

    public String getPenyidik() { return penyidik; }

    public String getJaksa() { return jaksa; }

    public List<Tersangka> getTersangkas() { return tersangkas; }

    public String getTersangkaNames() {
        if (tersangkas != null && !tersangkas.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tersangkas.size(); i++) {
                sb.append(tersangkas.get(i).getNama());
                if (i < tersangkas.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
        return "-";
    }
}