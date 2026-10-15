package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReminderResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private ReminderData data;

    public boolean isSuccess() {
        return success;
    }

    public ReminderData getData() {
        return data;
    }

    public static class ReminderData {
        @SerializedName("current_page")
        private int currentPage;

        @SerializedName("data")
        private List<Reminder> data;

        @SerializedName("total")
        private int total;

        @SerializedName("last_page")
        private int lastPage;

        @SerializedName("per_page")
        private int perPage;

        @SerializedName("next_page_url")
        private String nextPageUrl;

        public int getCurrentPage() {
            return currentPage;
        }

        public List<Reminder> getData() {
            return data;
        }

        public int getTotal() {
            return total;
        }

        public int getLastPage() {
            return lastPage;
        }

        public int getPerPage() {
            return perPage;
        }

        public String getNextPageUrl() {
            return nextPageUrl;
        }
    }
}
