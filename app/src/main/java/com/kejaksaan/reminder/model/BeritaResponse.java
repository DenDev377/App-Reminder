package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model respon API GET /api/berita.
 */
public class BeritaResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<Berita> data;

    @SerializedName("pagination")
    private BeritaPagination pagination;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Berita> getData() {
        return data;
    }

    public void setData(List<Berita> data) {
        this.data = data;
    }

    public BeritaPagination getPagination() {
        return pagination;
    }

    public void setPagination(BeritaPagination pagination) {
        this.pagination = pagination;
    }
}

