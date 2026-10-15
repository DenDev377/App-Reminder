package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PerkaraResponse;
import com.kejaksaan.reminder.model.PerkaraTimelineResponse;
import com.kejaksaan.reminder.repository.TimelineRepository;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimelineViewModel extends AndroidViewModel {

    private final TimelineRepository repository;

    // Timeline Perkara List (1 Card per Perkara)
    private final MutableLiveData<ApiResult<List<Perkara>>> perkarasResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPaginating = new MutableLiveData<>(false);
    private final MutableLiveData<String> paginationError = new MutableLiveData<>();

    // Per-Perkara Timeline Detail LiveData
    private final MutableLiveData<ApiResult<PerkaraTimelineResponse>> perkaraTimelineResult = new MutableLiveData<>();

    private final List<Perkara> accumulatedPerkaras = new ArrayList<>();
    private final Set<Integer> existingPerkaraIds = new HashSet<>();

    private String searchKeyword = "";
    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    public TimelineViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TimelineRepository(application);
    }

    public LiveData<ApiResult<List<Perkara>>> getPerkarasResult() {
        return perkarasResult;
    }

    public LiveData<Boolean> getIsPaginating() {
        return isPaginating;
    }

    public LiveData<String> getPaginationError() {
        return paginationError;
    }

    public LiveData<ApiResult<PerkaraTimelineResponse>> getPerkaraTimelineResult() {
        return perkaraTimelineResult;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    // --- Timeline Perkara List Methods ---

    /**
     * Memuat halaman pertama daftar perkara (1 card per perkara).
     */
    public void loadFirstPage(String search) {
        this.searchKeyword = search != null ? search.trim() : "";
        this.currentPage = 1;
        this.lastPage = 1;
        this.isLastPage = false;
        this.isLoading = true;

        this.accumulatedPerkaras.clear();
        this.existingPerkaraIds.clear();

        perkarasResult.setValue(ApiResult.loading());

        fetchPage(1, false);
    }

    /**
     * Memuat halaman berikutnya saat scroll mendekati bawah.
     */
    public void loadNextPage() {
        if (isLoading || isLastPage) {
            return;
        }

        int nextPage = currentPage + 1;
        if (nextPage > lastPage) {
            isLastPage = true;
            return;
        }

        this.isLoading = true;
        isPaginating.setValue(true);

        fetchPage(nextPage, true);
    }

    /**
     * Refresh halaman saat ini.
     */
    public void refresh() {
        loadFirstPage(searchKeyword);
    }

    private void fetchPage(int pageToFetch, boolean isPaginationCall) {
        MutableLiveData<ApiResult<PerkaraResponse.PerkaraPage>> pageResult = new MutableLiveData<>();
        pageResult.observeForever(result -> {
            if (result == null || result.isLoading()) {
                return;
            }

            isLoading = false;
            if (isPaginationCall) {
                isPaginating.setValue(false);
            }

            if (result.isSuccess() && result.getData() != null) {
                PerkaraResponse.PerkaraPage data = result.getData();
                currentPage = data.getCurrentPage() > 0 ? data.getCurrentPage() : pageToFetch;
                lastPage = data.getLastPage() > 0 ? data.getLastPage() : 1;
                isLastPage = currentPage >= lastPage;

                List<Perkara> newItems = data.getData();
                if (newItems != null) {
                    for (Perkara item : newItems) {
                        if (item != null && !existingPerkaraIds.contains(item.getId())) {
                            existingPerkaraIds.add(item.getId());
                            accumulatedPerkaras.add(item);
                        }
                    }
                }

                perkarasResult.setValue(ApiResult.success(new ArrayList<>(accumulatedPerkaras)));
            } else if (result.isError()) {
                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat data";
                if (isPaginationCall) {
                    paginationError.setValue(errorMsg);
                } else {
                    perkarasResult.setValue(ApiResult.error(errorMsg));
                }
            }
        });

        repository.getPerkarasForTimeline(pageToFetch, searchKeyword, pageResult);
    }

    // --- Detail Timeline Per-Perkara ---

    /**
     * Memuat seluruh timeline kronologis untuk satu perkara tertentu.
     */
    public void loadPerkaraTimeline(int perkaraId) {
        repository.getPerkaraTimeline(perkaraId, perkaraTimelineResult);
    }
}