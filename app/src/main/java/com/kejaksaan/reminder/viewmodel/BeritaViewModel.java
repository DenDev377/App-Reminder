package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.Berita;
import com.kejaksaan.reminder.model.BeritaPagination;
import com.kejaksaan.reminder.model.BeritaResponse;
import com.kejaksaan.reminder.repository.BeritaRepository;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BeritaViewModel — ViewModel untuk daftar berita dan dashboard preview.
 */
public class BeritaViewModel extends AndroidViewModel {

    private final BeritaRepository beritaRepository;

    private final MutableLiveData<ApiResult<List<Berita>>> beritasResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPaginating = new MutableLiveData<>(false);
    private final MutableLiveData<String> paginationError = new MutableLiveData<>();

    private final MutableLiveData<ApiResult<List<Berita>>> dashboardBeritasResult = new MutableLiveData<>();

    private final List<Berita> accumulatedBeritas = new ArrayList<>();
    private final Set<Integer> existingBeritaIds = new HashSet<>();

    private String searchKeyword = "";
    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int DEFAULT_PER_PAGE = 10;

    public BeritaViewModel(@NonNull Application application) {
        super(application);
        beritaRepository = new BeritaRepository(application);
    }

    public LiveData<ApiResult<List<Berita>>> getBeritasResult() {
        return beritasResult;
    }

    public LiveData<Boolean> getIsPaginating() {
        return isPaginating;
    }

    public LiveData<String> getPaginationError() {
        return paginationError;
    }

    public LiveData<ApiResult<List<Berita>>> getDashboardBeritasResult() {
        return dashboardBeritasResult;
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

    /**
     * Memuat berita halaman pertama (bisa dengan query pencarian).
     */
    public void loadFirstPage(String search) {
        this.searchKeyword = search != null ? search.trim() : "";
        this.currentPage = 1;
        this.lastPage = 1;
        this.isLastPage = false;
        this.isLoading = true;

        this.accumulatedBeritas.clear();
        this.existingBeritaIds.clear();

        beritasResult.setValue(ApiResult.loading());

        fetchPage(1, false);
    }

    /**
     * Memuat halaman berikutnya untuk infinite scrolling.
     */
    public void loadNextPage() {
        if (isLoading || isLastPage) {
            return;
        }

        isLoading = true;
        isPaginating.setValue(true);

        fetchPage(currentPage + 1, true);
    }

    /**
     * Segarkan halaman pertama dengan kata kunci yang saat ini aktif.
     */
    public void refresh() {
        loadFirstPage(this.searchKeyword);
    }

    /**
     * Memuat berita ringkas khusus untuk dashboard carousel horizontal.
     */
    public void loadDashboardBeritas(int count) {
        MutableLiveData<ApiResult<BeritaResponse>> tempLiveData = new MutableLiveData<>();
        tempLiveData.observeForever(apiResult -> {
            if (apiResult == null) return;
            if (apiResult.isLoading()) {
                dashboardBeritasResult.setValue(ApiResult.loading());
            } else if (apiResult.isSuccess() && apiResult.getData() != null) {
                List<Berita> data = apiResult.getData().getData();
                dashboardBeritasResult.setValue(ApiResult.success(data != null ? data : new ArrayList<>()));
            } else if (apiResult.isError()) {
                dashboardBeritasResult.setValue(ApiResult.error(apiResult.getMessage()));
            }
        });

        beritaRepository.getBeritas(1, count, null, tempLiveData);
    }

    private void fetchPage(int page, boolean isAppending) {
        MutableLiveData<ApiResult<BeritaResponse>> pageLiveData = new MutableLiveData<>();
        pageLiveData.observeForever(apiResult -> {
            if (apiResult == null) return;

            if (apiResult.isSuccess() && apiResult.getData() != null) {
                BeritaResponse response = apiResult.getData();
                List<Berita> newItems = response.getData() != null ? response.getData() : new ArrayList<>();

                BeritaPagination pagination = response.getPagination();
                if (pagination != null) {
                    currentPage = pagination.getCurrentPage();
                    lastPage = pagination.getLastPage();
                    isLastPage = currentPage >= lastPage || !pagination.isHasMore();
                } else {
                    currentPage = page;
                    isLastPage = newItems.isEmpty() || newItems.size() < DEFAULT_PER_PAGE;
                }

                // Hindari duplikasi item
                for (Berita b : newItems) {
                    if (b != null && !existingBeritaIds.contains(b.getId())) {
                        existingBeritaIds.add(b.getId());
                        accumulatedBeritas.add(b);
                    }
                }

                beritasResult.setValue(ApiResult.success(new ArrayList<>(accumulatedBeritas)));
                isLoading = false;
                if (isAppending) {
                    isPaginating.setValue(false);
                }

            } else if (apiResult.isError()) {
                isLoading = false;
                if (isAppending) {
                    isPaginating.setValue(false);
                    paginationError.setValue(apiResult.getMessage());
                } else {
                    beritasResult.setValue(ApiResult.error(apiResult.getMessage()));
                }
            }
        });

        beritaRepository.getBeritas(page, DEFAULT_PER_PAGE, searchKeyword, pageLiveData);
    }
}

