package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model response untuk endpoint GET /api/berita/{id}
 * yang mengembalikan satu objek Berita tunggal.
 */
public class BeritaSingleResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Berita data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Berita getData() {
        return data;
    }
}
