package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Wrapper untuk respons GET /api/perkara (paginated):
 * {
 *   "success": true,
 *   "data": {
 *     "current_page": 1,
 *     "data": [ PerkaraObject, ... ],
 *     "last_page": 5,
 *     "total": 50
 *   }
 * }
 */
public class PerkaraResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private PerkaraPage data;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public boolean isSuccess() { return success; }

    public PerkaraPage getData() { return data; }

    /** Convenience: langsung ambil list perkara */
    public List<Perkara> getPerkaraList() {
        return data != null ? data.getData() : null;
    }

    // ─── Inner class: PerkaraPage (Laravel Paginator) ────────────────────────────

    public static class PerkaraPage {

        @SerializedName("current_page")
        private int currentPage;

        @SerializedName("data")
        private List<Perkara> data;

        @SerializedName("last_page")
        private int lastPage;

        @SerializedName("total")
        private int total;

        @SerializedName("per_page")
        private int perPage;

        public int getCurrentPage() { return currentPage; }

        public List<Perkara> getData() { return data; }

        public int getLastPage() { return lastPage; }

        public int getTotal() { return total; }

        public int getPerPage() { return perPage; }
    }
}
