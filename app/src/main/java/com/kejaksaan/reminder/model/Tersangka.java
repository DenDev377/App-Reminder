package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Model Tersangka — bagian dari PerkaraObject.
 * Field bisa ditambah sesuai struktur backend.
 */
public class Tersangka {

    @SerializedName("id")
    private int id;

    @SerializedName("nama")
    private String nama;

    @SerializedName("nik")
    private String nik;

    @SerializedName("alamat")
    private String alamat;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getId() { return id; }

    public String getNama() { return nama; }

    public String getNik() { return nik; }

    public String getAlamat() { return alamat; }
}
