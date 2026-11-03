package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Model paginasi untuk respon berita.
 */
public class BeritaPagination implements Serializable {

    @SerializedName("current_page")
    private int currentPage;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("total")
    private int total;

    @SerializedName("last_page")
    private int lastPage;

    @SerializedName("has_more")
    private boolean hasMore;

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getLastPage() {
        return lastPage;
    }

    public void setLastPage(int lastPage) {
        this.lastPage = lastPage;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}

