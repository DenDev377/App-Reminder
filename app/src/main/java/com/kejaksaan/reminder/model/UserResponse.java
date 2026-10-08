package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Wrapper untuk respons GET /api/user:
 * {
 *   "success": true,
 *   "data": {
 *     "user": { id, name, email, role, nip, pangkat_golongan, jabatan, foto_profil }
 *   }
 * }
 */
public class UserResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private UserData data;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public boolean isSuccess() { return success; }

    public UserData getData() { return data; }

    /** Convenience: ambil User langsung */
    public User getUser() {
        return data != null ? data.getUser() : null;
    }

    // ─── Inner class: UserData ───────────────────────────────────────────────────

    public static class UserData {

        @SerializedName("user")
        private User user;

        public User getUser() { return user; }
    }
}
