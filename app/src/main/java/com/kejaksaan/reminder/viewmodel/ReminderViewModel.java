package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.Reminder;
import com.kejaksaan.reminder.model.ReminderResponse;
import com.kejaksaan.reminder.repository.ReminderRepository;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReminderViewModel extends AndroidViewModel {

    private final ReminderRepository repository;

    private final MutableLiveData<ApiResult<List<Reminder>>> remindersResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPaginating = new MutableLiveData<>(false);
    private final MutableLiveData<String> paginationError = new MutableLiveData<>();

    private final List<Reminder> accumulatedReminders = new ArrayList<>();
    private final Set<Integer> existingIds = new HashSet<>();

    private String currentUrgensi = null;
    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    public ReminderViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ReminderRepository(application);
    }

    public LiveData<ApiResult<List<Reminder>>> getRemindersResult() {
        return remindersResult;
    }

    public LiveData<Boolean> getIsPaginating() {
        return isPaginating;
    }

    public LiveData<String> getPaginationError() {
        return paginationError;
    }

    public String getCurrentUrgensi() {
        return currentUrgensi;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Memuat halaman pertama (reset daftar & filter).
     *
     * @param urgensi Filter urgensi (null jika semua)
     */
    public void loadFirstPage(String urgensi) {
        this.currentUrgensi = urgensi;
        this.currentPage = 1;
        this.lastPage = 1;
        this.isLastPage = false;
        this.isLoading = true;

        this.accumulatedReminders.clear();
        this.existingIds.clear();

        remindersResult.setValue(ApiResult.loading());

        fetchPage(1, false);
    }

    /**
     * Memuat halaman berikutnya saat pengguna menggulir mendekati akhir daftar.
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
     * Refresh halaman saat ini dengan filter yang sedang aktif.
     */
    public void refresh() {
        loadFirstPage(currentUrgensi);
    }

    private void fetchPage(int pageToFetch, boolean isPaginationCall) {
        MutableLiveData<ApiResult<ReminderResponse.ReminderData>> pageResult = new MutableLiveData<>();
        pageResult.observeForever(new androidx.lifecycle.Observer<ApiResult<ReminderResponse.ReminderData>>() {
            @Override
            public void onChanged(ApiResult<ReminderResponse.ReminderData> result) {
                if (result == null || result.isLoading()) {
                    return;
                }

                isLoading = false;
                if (isPaginationCall) {
                    isPaginating.setValue(false);
                }

                if (result.isSuccess() && result.getData() != null) {
                    ReminderResponse.ReminderData data = result.getData();
                    currentPage = data.getCurrentPage() > 0 ? data.getCurrentPage() : pageToFetch;
                    lastPage = data.getLastPage() > 0 ? data.getLastPage() : 1;
                    isLastPage = currentPage >= lastPage;

                    List<Reminder> newItems = data.getData();
                    if (newItems != null) {
                        for (Reminder r : newItems) {
                            if (r != null && !existingIds.contains(r.getId())) {
                                existingIds.add(r.getId());
                                accumulatedReminders.add(r);
                            }
                        }
                    }

                    remindersResult.setValue(ApiResult.success(new ArrayList<>(accumulatedReminders)));
                } else if (result.isError()) {
                    String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat data";
                    if (isPaginationCall) {
                        paginationError.setValue(errorMsg);
                    } else {
                        remindersResult.setValue(ApiResult.error(errorMsg));
                    }
                }
            }
        });

        repository.getRemindersPaged(currentUrgensi, pageToFetch, pageResult);
    }
}
