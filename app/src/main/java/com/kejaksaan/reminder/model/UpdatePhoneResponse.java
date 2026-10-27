package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model response dari endpoint update nomor HP:
 * POST /api/profile/nomor-hp
 *
 * Contoh response:
 * {
 *   "success": true,
 *   "message": "Nomor HP berhasil diperbarui",
 *   "data": {
 *     "nomor_hp": "081234567890",
 *     "user": { ... }
 *   }
 * }
 */
public class UpdatePhoneResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public static class Data {

        @SerializedName("nomor_hp")
        private String nomorHp;

        @SerializedName("user")
        private User user;

        public String getNomorHp() {
            return nomorHp;
        }

        public User getUser() {
            return user;
        }
    }
}
