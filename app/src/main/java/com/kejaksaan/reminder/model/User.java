package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model User sesuai API response:
 * { "id": 1, "name": "...", "email": "...", "role": "jaksa",
 *   "nip": "...", "nrp": "...", "nomor_hp": "...",
 *   "pangkat_golongan": "IV/a", "jabatan": "Ketua",
 *   "foto_profil": "folder/image.jpg",
 *   "foto_profil_url": "http://...storage/folder/image.jpg" }
 */
public class User {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("nip")
    private String nip;

    @SerializedName("nrp")
    private String nrp;

    @SerializedName("nomor_hp")
    private String nomorHp;

    @SerializedName("pangkat_golongan")
    private String pangkatGolongan;

    @SerializedName("jabatan")
    private String jabatan;

    @SerializedName("foto_profil")
    private String fotoProfil;

    @SerializedName("foto_profil_url")
    private String fotoProlilUrl;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getRole() { return role; }

    public String getNip() { return nip; }

    public String getNrp() { return nrp; }

    public String getNomorHp() { return nomorHp; }

    public String getPangkatGolongan() { return pangkatGolongan; }

    public String getJabatan() { return jabatan; }

    public String getFotoProfil() { return fotoProfil; }

    /**
     * URL lengkap foto profil dari server (sudah include base URL storage).
     * Gunakan ini untuk load gambar via Glide.
     */
    public String getFotoProlilUrl() { return fotoProlilUrl; }

    /**
     * Kembalikan URL foto yang siap digunakan: prioritaskan foto_profil_url,
     * fallback ke konstruksi dari foto_profil + base URL.
     */
    public String getResolvedPhotoUrl() {
        if (fotoProlilUrl != null && !fotoProlilUrl.isEmpty()) {
            return fotoProlilUrl;
        }
        if (fotoProfil != null && !fotoProfil.isEmpty()) {
            if (fotoProfil.startsWith("http")) return fotoProfil;
            return com.kejaksaan.reminder.api.RetrofitClient.BASE_URL + "storage/" + fotoProfil;
        }
        return null;
    }
}
