package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TimelinePagination {

    @SerializedName("current_page")
    private int currentPage;

    @SerializedName("data")
    private List<PerkaraTimeline> data;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("total")
    private int total;

    @SerializedName("last_page")
    private int lastPage;

    @SerializedName("next_page_url")
    private String nextPageUrl;

    public int getCurrentPage() {
        return currentPage;
    }

    public List<PerkaraTimeline> getData() {
        return data;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTotal() {
        return total;
    }

    public int getLastPage() {
        return lastPage;
    }

    public String getNextPageUrl() {
        return nextPageUrl;
    }
}