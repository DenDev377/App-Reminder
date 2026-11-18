package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model request untuk update profil pegawai:
 * Nama, NIP, Pangkat/Golongan, dan Jabatan.
 */
public class UpdateProfileRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("nip")
    private String nip;

    @SerializedName("nrp")
    private String nrp;

    @SerializedName("pangkat_golongan")
    private String pangkatGolongan;

    @SerializedName("pangkat")
    private String pangkat;

    @SerializedName("jabatan")
    private String jabatan;

    public UpdateProfileRequest(String name, String nip, String nrp, String pangkatGolongan, String jabatan) {
        this.name = name;
        this.nip = nip;
        this.nrp = nrp;
        this.pangkatGolongan = pangkatGolongan;
        this.pangkat = pangkatGolongan;
        this.jabatan = jabatan;
    }

    public UpdateProfileRequest(String name, String nip, String pangkatGolongan, String jabatan) {
        this(name, nip, null, pangkatGolongan, jabatan);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public String getNrp() {
        return nrp;
    }

    public void setNrp(String nrp) {
        this.nrp = nrp;
    }

    public String getPangkatGolongan() {
        return pangkatGolongan;
    }

    public void setPangkatGolongan(String pangkatGolongan) {
        this.pangkatGolongan = pangkatGolongan;
        this.pangkat = pangkatGolongan;
    }

    public String getPangkat() {
        return pangkat;
    }

    public String getJabatan() {
        return jabatan;
    }

    public void setJabatan(String jabatan) {
        this.jabatan = jabatan;
    }
}

