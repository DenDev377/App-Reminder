package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Wrapper untuk respons GET /api/user:
 * Mendukung format bersarang data.user, data langsung sebagai user, maupun root user.
 */
public class UserResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private UserData data;

    @SerializedName("user")
    private User rootUser;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserData getData() {
        return data;
    }

    /** Convenience: ambil User langsung dengan fallback fleksibel */
    public User getUser() {
        if (data != null && data.getUser() != null) {
            return data.getUser();
        }
        if (rootUser != null) {
            return rootUser;
        }
        return null;
    }

    // ─── Inner class: UserData ───────────────────────────────────────────────────

    public static class UserData {

        @SerializedName("user")
        private User user;

        // Jika backend mengembalikan atribut user langsung di dalam objek data
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
        private String fotoProfilUrl;
        @SerializedName("foto_profile_url")
        private String fotoProfileUrl;

        public User getUser() {
            if (user != null) {
                return user;
            }
            if (id > 0 || (name != null && !name.isEmpty())) {
                User u = new User();
                u.setName(name);
                u.setNip(nip);
                u.setPangkatGolongan(pangkatGolongan);
                u.setJabatan(jabatan);
                u.setNomorHp(nomorHp);
                u.setFotoProfil(fotoProfil);
                u.setFotoProfilUrl(fotoProfilUrl);
                u.setFotoProfileUrl(fotoProfileUrl);
                return u;
            }
            return null;
        }
    }
}
