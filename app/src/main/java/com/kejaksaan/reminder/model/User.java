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

    @SerializedName("foto_profile_url")
    private String fotoProfileUrl;

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

    public String getFotoProfileUrl() { return fotoProfileUrl; }

    /**
     * Kembalikan URL foto yang siap digunakan: prioritaskan foto_profile_url / foto_profil_url,
     * fallback ke konstruksi dari foto_profil + base URL.
     * Secara otomatis menyesuaikan jika server mengembalikan host internal/localhost yang berbeda dari client.
     */
    public String getResolvedPhotoUrl() {
        String url = (fotoProfileUrl != null && !fotoProfileUrl.isEmpty())
                ? fotoProfileUrl
                : fotoProlilUrl;

        if (url == null || url.trim().isEmpty()) {
            url = fotoProfil;
        }

        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        url = url.trim();

        // Jika path relatif
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.startsWith("/")) {
                url = url.substring(1);
            }
            if (url.startsWith("storage/")) {
                return com.kejaksaan.reminder.api.RetrofitClient.BASE_URL + url;
            }
            return com.kejaksaan.reminder.api.RetrofitClient.BASE_URL + "storage/" + url;
        }

        // Jika URL lengkap, pastikan host selaras jika server mengembalikan localhost/127.0.0.1
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1"))) {
                java.net.URI baseUri = new java.net.URI(com.kejaksaan.reminder.api.RetrofitClient.BASE_URL);
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) path = path.substring(1);
                return com.kejaksaan.reminder.api.RetrofitClient.BASE_URL + (path != null ? path : "");
            }
        } catch (Exception ignored) {
        }

        return url;
    }

    // ─── Setters ────────────────────────────────────────────────────────────────

    public void setName(String name) { this.name = name; }
    public void setNip(String nip) { this.nip = nip; }
    public void setPangkatGolongan(String pangkatGolongan) { this.pangkatGolongan = pangkatGolongan; }
    public void setJabatan(String jabatan) { this.jabatan = jabatan; }
    public void setNomorHp(String nomorHp) { this.nomorHp = nomorHp; }
    public void setFotoProfil(String fotoProfil) { this.fotoProfil = fotoProfil; }
    public void setFotoProfilUrl(String fotoProfilUrl) { this.fotoProlilUrl = fotoProfilUrl; }
    public void setFotoProfileUrl(String fotoProfileUrl) { this.fotoProfileUrl = fotoProfileUrl; }
}
