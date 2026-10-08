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

        public int getCurrentPage() {
            return currentPage;
        }

        public List<Reminder> getData() {
            return data;
        }

        public int getTotal() {
            return total;
        }
    }
}
