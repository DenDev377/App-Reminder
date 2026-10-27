package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body untuk POST /api/perkara/{id}/actions.
 */
public class ProcessActionRequest {

    @SerializedName("action_id")
    private String actionId;

    @SerializedName("tanggal")
    private String tanggal;

    @SerializedName("catatan")
    private String catatan;

    public ProcessActionRequest() {
    }

    public ProcessActionRequest(String actionId, String tanggal, String catatan) {
        this.actionId = actionId;
        this.tanggal = tanggal;
        this.catatan = catatan;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getTanggal() {
        return tanggal;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }
}
