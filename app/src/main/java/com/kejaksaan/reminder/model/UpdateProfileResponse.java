package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model response untuk update profil pegawai.
 *
 * Contoh payload response:
 * {
 *   "success": true,
 *   "message": "Profil berhasil diperbarui",
 *   "data": {
 *     "user": { ... }
 *   }
 * }
 */
public class UpdateProfileResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    // Kadang backend langsung mengembalikan user di root response
    @SerializedName("user")
    private User rootUser;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public User getUser() {
        User u = null;
        if (data != null && data.getUser() != null) {
            u = data.getUser();
        } else if (rootUser != null) {
            u = rootUser;
        }

        // Jika user ada tapi data memiliki foto_profil_url/foto_profile_url
        if (u != null && data != null) {
            if (u.getResolvedPhotoUrl() == null && data.getFotoProfilUrl() != null) {
                u.setFotoProfileUrl(data.getFotoProfilUrl());
            }
            if (u.getFotoProfil() == null && data.getFotoProfil() != null) {
                u.setFotoProfil(data.getFotoProfil());
            }
        }
        return u;
    }

    public static class Data {
        @SerializedName("user")
        private User user;

        @SerializedName("foto_profil")
        private String fotoProfil;

        @SerializedName("foto_profil_url")
        private String fotoProfilUrl;

        @SerializedName("foto_profile_url")
        private String fotoProfileUrl;

        public User getUser() {
            return user;
        }

        public String getFotoProfil() {
            return fotoProfil;
        }

        public String getFotoProfilUrl() {
            return (fotoProfileUrl != null && !fotoProfileUrl.isEmpty())
                    ? fotoProfileUrl
                    : fotoProfilUrl;
        }
    }
}

