package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PerkaraTimelineResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("perkara")
    private Perkara perkara;

    @SerializedName("data")
    private List<PerkaraTimeline> data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Perkara getPerkara() {
        return perkara;
    }

    public List<PerkaraTimeline> getData() {
        return data;
    }
}