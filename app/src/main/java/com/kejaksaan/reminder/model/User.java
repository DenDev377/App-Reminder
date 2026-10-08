package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model User sesuai API response:
 * { "id": 1, "name": "...", "email": "...", "role": "jaksa",
 *   "nip": "123", "pangkat_golongan": "IV/a", "jabatan": "Ketua",
 *   "foto_profil": "folder/image.jpg" }
 */
public class User {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("nip")
    private String nip;

    @SerializedName("pangkat_golongan")
    private String pangkatGolongan;

    @SerializedName("jabatan")
    private String jabatan;

    @SerializedName("foto_profil")
    private String fotoProfil;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getRole() { return role; }

    public String getNip() { return nip; }

    public String getPangkatGolongan() { return pangkatGolongan; }

    public String getJabatan() { return jabatan; }

    public String getFotoProfil() { return fotoProfil; }
}
