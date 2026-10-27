package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model untuk merepresentasikan sebuah tindakan workflow alur perkara
 * yang diperoleh dari endpoint GET /api/perkara/{id}/available-actions.
 */
public class WorkflowAction {

    @SerializedName("id")
    private String id;

    @SerializedName("label")
    private String label;

    public WorkflowAction() {
    }

    public WorkflowAction(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Mengembalikan judul tindakan yang spesifik dan resmi sesuai istilah SOP Kejaksaan.
     */
    public String getDisplayTitle() {
        if (id == null) return label != null ? label : "Tindakan Perkara";

        switch (id) {
            case "koordinasi_selesai":
                return "Selesaikan Koordinasi";
            case "catat_p17":
                return "Catat Pengiriman P-17";
            case "catat_p17_2":
                return "Catat Pengiriman SOP FORM-2 (P-17 Ke-2)";
            case "catat_form3":
                return "Terbitkan SOP FORM-3 (Pengembalian SPDP)";
            case "terima_berkas":
                return "Konfirmasi Penerimaan Berkas Tahap I";
            case "hasil_lengkap":
                return "Proses Berkas Lengkap (P-21)";
            case "hasil_belum_lengkap":
                return "Proses Berkas Belum Lengkap (P-19)";
            case "terima_pemenuhan_p19":
                return "Terima Berkas Pemenuhan Petunjuk (P-19)";
            case "p20":
                return "Terbitkan P-20 (Pengembalian SPDP)";
            case "terima_tahap2":
                return "Konfirmasi Penerimaan Tahap II";
            case "form7":
                return "Terbitkan SOP FORM-7";
            case "tahap2_selesai":
                return "Selesaikan Pelaksanaan Tahap II";
            default:
                return (label != null && !label.isEmpty()) ? label : id;
        }
    }

    /**
     * Keterangan singkat satu kalimat yang menjelaskan maksud tindakan SOP.
     */
    public String getDisplayDescription() {
        if (id == null) return "Proses tindakan alur perkara sesuai prosedur.";

        switch (id) {
            case "koordinasi_selesai":
                return "Menyelesaikan koordinasi awal dan memulai monitoring perkembangan penyidikan berkas.";
            case "catat_p17":
                return "Mencatat penerbitan surat P-17 untuk meminta perkembangan penyidikan kepada penyidik.";
            case "catat_p17_2":
                return "Mencatat penerbitan surat peringatan kedua (SOP FORM-2) atas permintaan perkembangan penyidikan.";
            case "catat_form3":
                return "Mengembalikan SPDP ke penyidik dan menyelesaikan penanganan berkas (SOP FORM-3).";
            case "terima_berkas":
                return "Mencatat penerimaan berkas perkara tahap pertama dari penyidik untuk diteliti (Tahap P-24).";
            case "hasil_lengkap":
                return "Menyatakan hasil penelitian berkas telah lengkap memenuhi syarat formil dan materiil (P-21).";
            case "hasil_belum_lengkap":
                return "Menyatakan berkas belum lengkap dan menerbitkan petunjuk P-18/P-19 kepada penyidik.";
            case "terima_pemenuhan_p19":
                return "Mencatat penerimaan berkas perkara perbaikan dari penyidik untuk diteliti kembali.";
            case "p20":
                return "Menerbitkan surat P-20 dan mengembalikan SPDP karena batas waktu pemenuhan habis.";
            case "terima_tahap2":
                return "Mencatat penyerahan tanggung jawab tersangka dan barang bukti dari penyidik ke Jaksa.";
            case "form7":
                return "Menerbitkan SOP FORM-7 untuk mengembalikan berkas karena penyidik tidak menyerahkan Tahap II.";
            case "tahap2_selesai":
                return "Menyelesaikan seluruh rangkaian penerimaan Tahap II untuk administrasi penuntutan.";
            default:
                return "Melanjutkan alur perkara sesuai tahapan SOP Kejaksaan Republik Indonesia.";
        }
    }

    /**
     * Menjelaskan estimasi dampak perubahan tahap/status alur perkara.
     */
    public String getNextImpactSummary() {
        if (id == null) return "Memperbarui status alur perkara";

        switch (id) {
            case "koordinasi_selesai":
                return "Tahap berikutnya: Monitoring Berkas — Status: Menunggu Berkas Tahap I";
            case "catat_p17":
                return "Tahap: Monitoring Berkas — Status: P-17";
            case "catat_p17_2":
                return "Tahap: Monitoring Berkas — Status: P-17 Kedua / FORM-2";
            case "catat_form3":
                return "Tahap: Monitoring Berkas — Status: SOP FORM-3 (Pengembalian SPDP)";
            case "terima_berkas":
                return "Tahap berikutnya: P-24 — Status: Menunggu Penelitian Berkas";
            case "hasil_lengkap":
                return "Tahap berikutnya: P-21 — Status: Menunggu Penyerahan Tahap II";
            case "hasil_belum_lengkap":
                return "Tahap berikutnya: P-19 — Status: Menunggu Pemenuhan Petunjuk";
            case "terima_pemenuhan_p19":
                return "Tahap berikutnya: P-24 — Status: Menunggu Penelitian Berkas";
            case "p20":
                return "Tahap: P-19 — Status: P-20 (Pengembalian SPDP)";
            case "terima_tahap2":
                return "Tahap berikutnya: Tahap II — Status: Menunggu Pelaksanaan Tahap II";
            case "form7":
                return "Tahap: P-21 — Status: SOP FORM-7 (Pengembalian Berkas & SPDP)";
            case "tahap2_selesai":
                return "Tahap: Tahap II — Status: Tahap II Selesai";
            default:
                return "Pembaruan alur perkara terkait";
        }
    }

    /**
     * Teks spesifik untuk tombol eksekusi (menghindari tombol generik 'Ya' / 'Sudah').
     */
    public String getActionButtonText() {
        if (id == null) return label != null ? label : "Proses Tindakan";

        switch (id) {
            case "koordinasi_selesai":
                return "Selesaikan Koordinasi";
            case "catat_p17":
                return "Catat Pengiriman P-17";
            case "catat_p17_2":
                return "Catat Kirim FORM-2";
            case "catat_form3":
                return "Terbitkan SOP FORM-3";
            case "terima_berkas":
                return "Konfirmasi Penerimaan Berkas";
            case "hasil_lengkap":
                return "Proses Berkas Lengkap (P-21)";
            case "hasil_belum_lengkap":
                return "Proses Berkas Belum Lengkap (P-19)";
            case "terima_pemenuhan_p19":
                return "Terima Pemenuhan Petunjuk";
            case "p20":
                return "Terbitkan P-20";
            case "terima_tahap2":
                return "Konfirmasi Penerimaan Tahap II";
            case "form7":
                return "Terbitkan SOP FORM-7";
            case "tahap2_selesai":
                return "Selesaikan Tahap II";
            default:
                return (label != null && !label.isEmpty()) ? label : "Proses Tindakan";
        }
    }

    /**
     * Warna aksen identifikasi visual (Teal, Emerald, Amber, Carmine).
     */
    public String getAccentColorHex() {
        if (id == null) return "#00897B";

        switch (id) {
            case "hasil_lengkap":
                return "#059669"; // Emerald hijau segar untuk P-21 Lengkap
            case "hasil_belum_lengkap":
            case "catat_p17":
            case "catat_p17_2":
                return "#D97706"; // Amber hangat untuk P-19 dan P-17
            case "catat_form3":
            case "p20":
            case "form7":
                return "#DC2626"; // Merah tegas untuk pengembalian / batas waktu
            case "koordinasi_selesai":
            case "terima_berkas":
            case "terima_pemenuhan_p19":
            case "terima_tahap2":
            case "tahap2_selesai":
            default:
                return "#00897B"; // Teal standar resmi Kejaksaan
        }
    }

    /**
     * Badge label kategori SOP alur perkara.
     */
    public String getBadgeLabel() {
        if (id == null) return "SOP ALUR";

        switch (id) {
            case "koordinasi_selesai":
                return "KOORDINASI AWAL";
            case "catat_p17":
                return "MONITORING P-17";
            case "catat_p17_2":
                return "PERINGATAN FORM-2";
            case "catat_form3":
                return "PENGEMBALIAN SPDP";
            case "terima_berkas":
                return "PENYERAHAN TAHAP I";
            case "hasil_lengkap":
                return "HASIL P-21 (LENGKAP)";
            case "hasil_belum_lengkap":
                return "HASIL P-19 (PETUNJUK)";
            case "terima_pemenuhan_p19":
                return "PENELITIAN ULANG";
            case "p20":
                return "P-20 KEDALUWARSA";
            case "terima_tahap2":
                return "PENYERAHAN TAHAP II";
            case "form7":
                return "SOP FORM-7";
            case "tahap2_selesai":
                return "TAHAP II SELESAI";
            default:
                return "SOP PERKARA";
        }
    }
}
