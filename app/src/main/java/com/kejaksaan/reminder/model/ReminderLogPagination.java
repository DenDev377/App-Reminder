package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Struktur objek data paginasi untuk log reminder.
 */
public class ReminderLogPagination {

    @SerializedName("current_page")
    private int currentPage;

    @SerializedName("data")
    private List<ReminderLog> data;

    @SerializedName("last_page")
    private int lastPage;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("total")
    private int total;

    @SerializedName("next_page_url")
    private String nextPageUrl;

    public int getCurrentPage() {
        return currentPage;
    }

    public List<ReminderLog> getData() {
        return data;
    }

    public int getLastPage() {
        return lastPage;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTotal() {
        return total;
    }

    public String getNextPageUrl() {
        return nextPageUrl;
    }
}
