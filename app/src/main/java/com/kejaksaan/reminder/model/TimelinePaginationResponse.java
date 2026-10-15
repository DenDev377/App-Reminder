package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

public class TimelinePaginationResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private TimelinePagination data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public TimelinePagination getData() {
        return data;
    }
}