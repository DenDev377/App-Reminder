package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body untuk ganti kata sandi.
 * POST /api/user/change-password
 * Body: { "current_password": "...", "new_password": "...", "new_password_confirmation": "..." }
 */
public class ChangePasswordRequest {

    @SerializedName("current_password")
    private final String currentPassword;

    @SerializedName("new_password")
    private final String newPassword;

    @SerializedName("new_password_confirmation")
    private final String newPasswordConfirmation;

    public ChangePasswordRequest(String currentPassword, String newPassword, String newPasswordConfirmation) {
        this.currentPassword         = currentPassword;
        this.newPassword             = newPassword;
        this.newPasswordConfirmation = newPasswordConfirmation;
    }
}
