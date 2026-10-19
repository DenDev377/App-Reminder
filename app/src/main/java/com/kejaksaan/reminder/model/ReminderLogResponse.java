package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response model untuk endpoint GET /api/reminders/logs.
 */
public class ReminderLogResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("summary")
    private ReminderLogSummary summary;

    @SerializedName("data")
    private ReminderLogPagination data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public ReminderLogSummary getSummary() {
        return summary;
    }

    public ReminderLogPagination getData() {
        return data;
    }
}
