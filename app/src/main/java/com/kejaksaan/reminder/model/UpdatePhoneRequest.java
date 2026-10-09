package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body untuk update nomor HP.
 * PATCH /api/user/update-phone
 * Body: { "nomor_hp": "08123..." }
 */
public class UpdatePhoneRequest {

    @SerializedName("nomor_hp")
    private final String nomorHp;

    public UpdatePhoneRequest(String nomorHp) {
        this.nomorHp = nomorHp;
    }
}
