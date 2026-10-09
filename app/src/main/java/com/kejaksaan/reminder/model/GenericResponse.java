package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Generic response sederhana untuk operasi yang hanya mengembalikan success + message.
 * Contoh: { "success": true, "message": "Kata sandi berhasil diubah" }
 */
public class GenericResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
