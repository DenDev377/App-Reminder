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
        if (data != null && data.getUser() != null) {
            return data.getUser();
        }
        if (rootUser != null) {
            return rootUser;
        }
        return null;
    }

    public static class Data {
        @SerializedName("user")
        private User user;

        public User getUser() {
            return user;
        }
    }
}

