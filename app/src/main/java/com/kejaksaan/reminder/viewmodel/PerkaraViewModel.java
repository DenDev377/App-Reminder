package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PerkaraResponse;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.repository.PerkaraRepository;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PerkaraViewModel — ViewModel untuk layar Daftar Perkara & Profil.
 */
public class PerkaraViewModel extends AndroidViewModel {

    private final PerkaraRepository perkaraRepository;

    private final MutableLiveData<ApiResult<List<Perkara>>> perkarasResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPaginating = new MutableLiveData<>(false);
    private final MutableLiveData<String> paginationError = new MutableLiveData<>();

    private final MutableLiveData<ApiResult<User>>          userProfileResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<List<com.kejaksaan.reminder.model.Reminder>>> slaRemindersResult = new MutableLiveData<>();

    private final List<Perkara> accumulatedPerkaras = new ArrayList<>();
    private final Set<Integer> existingPerkaraIds = new HashSet<>();

    private String searchKeyword = "";
    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    public PerkaraViewModel(@NonNull Application application) {
        super(application);
        perkaraRepository = new PerkaraRepository(application);
    }

    // ─── Exposed LiveData ────────────────────────────────────────────────────────

    public LiveData<ApiResult<List<Perkara>>> getPerkarasResult() {
        return perkarasResult;
    }

    public LiveData<Boolean> getIsPaginating() {
        return isPaginating;
    }

    public LiveData<String> getPaginationError() {
        return paginationError;
    }

    public LiveData<ApiResult<User>> getUserProfileResult() {
        return userProfileResult;
    }

    public LiveData<ApiResult<List<com.kejaksaan.reminder.model.Reminder>>> getSlaRemindersResult() {
        return slaRemindersResult;
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

    // ─── Perkara List & Search Methods ───────────────────────────────────────────

    /**
     * Memuat halaman pertama daftar perkara dengan opsi kata kunci pencarian.
     *
     * @param search Kata kunci pencarian nomor perkara, nama tersangka, dll.
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
     * Overload: muat daftar perkara tanpa kata kunci.
     */
    public void loadPerkaras() {
        loadFirstPage("");
    }

    /**
     * Overload: muat daftar perkara dengan kata kunci.
     */
    public void loadPerkaras(String search) {
        loadFirstPage(search);
    }

    /**
     * Memuat halaman berikutnya saat scroll mencapai batas bawah.
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
     * Refresh halaman saat ini sesuai keyword pencarian yang aktif.
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
                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat data perkara";
                if (isPaginationCall) {
                    paginationError.setValue(errorMsg);
                } else {
                    perkarasResult.setValue(ApiResult.error(errorMsg));
                }
            }
        });

        perkaraRepository.getPerkaras(pageToFetch, searchKeyword, pageResult);
    }

    // ─── Profil & Reminders ──────────────────────────────────────────────────────

    /**
     * Muat profil user yang sedang login.
     */
    public void loadUserProfile() {
        perkaraRepository.getUserProfile(userProfileResult);
    }

    /**
     * Muat SLA Reminders
     */
    public void loadSlaReminders(String urgensi) {
        perkaraRepository.getSlaReminders(urgensi, slaRemindersResult);
    }

    /**
     * Getter halaman saat ini (untuk keperluan UI pagination).
     */
    public int getCurrentPage() {
        return currentPage;
    }
}
