package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Wrapper untuk respons login:
 * {
 *   "success": true,
 *   "message": "Login berhasil",
 *   "data": {
 *     "user": { ... },
 *     "token": "BearerTokenString"
 *   }
 * }
 */
public class LoginResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private LoginData data;

    // ─── Getters ─────────────────────────────────────────────────────────────────

    public boolean isSuccess() { return success; }

    public String getMessage() { return message; }

    public LoginData getData() { return data; }

    // ─── Convenience helpers ──────────────────────────────────────────────────────

    /** Mengambil token langsung dari data.token */
    public String getToken() {
        return data != null ? data.getToken() : null;
    }

    /** Mengambil User langsung dari data.user */
    public User getUser() {
        return data != null ? data.getUser() : null;
    }

    // ─── Inner class: LoginData ───────────────────────────────────────────────────

    public static class LoginData {

        @SerializedName("user")
        private User user;

        @SerializedName("token")
        private String token;

        public User getUser() { return user; }

        public String getToken() { return token; }
    }
}
