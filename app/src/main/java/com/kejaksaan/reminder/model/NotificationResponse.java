package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model response paginasi dari endpoint GET /api/notifications.
 */
public class NotificationResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private NotificationPagination data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public NotificationPagination getData() {
        return data;
    }

    public static class NotificationPagination {

        @SerializedName("current_page")
        private int currentPage;

        @SerializedName("data")
        private List<PushNotificationItem> data;

        @SerializedName("total")
        private int total;

        @SerializedName("last_page")
        private int lastPage;

        @SerializedName("per_page")
        private int perPage;

        public int getCurrentPage() {
            return currentPage;
        }

        public List<PushNotificationItem> getData() {
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
    }
}
