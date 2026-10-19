package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PerkaraReminderLogGroup;
import com.kejaksaan.reminder.model.ReminderLog;
import com.kejaksaan.reminder.model.ReminderLogPagination;
import com.kejaksaan.reminder.model.ReminderLogResponse;
import com.kejaksaan.reminder.model.ReminderLogSummary;
import com.kejaksaan.reminder.repository.ReminderLogRepository;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ViewModel untuk mengelola state dan data Audit Trail / Log Reminders SLA.
 * Mengelompokkan log reminder per perkara (1 card per nomor perkara/SPDP).
 */
public class ReminderLogViewModel extends AndroidViewModel {

    private final ReminderLogRepository repository;

    private final MutableLiveData<ApiResult<List<PerkaraReminderLogGroup>>> perkaraGroupsResult = new MutableLiveData<>();
    private final MutableLiveData<ReminderLogSummary> summaryResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPaginating = new MutableLiveData<>(false);
    private final MutableLiveData<String> paginationError = new MutableLiveData<>();

    // Map untuk mengelompokkan secara terurut 1 perkara -> 1 grup
    private final Map<String, PerkaraReminderLogGroup> perkaraGroupMap = new LinkedHashMap<>();
    private final Set<Integer> existingLogIds = new HashSet<>();

    private String currentStatus = "all";
    private String currentSearch = "";
    private String currentTahap = null;

    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int PER_PAGE = 20;

    public ReminderLogViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ReminderLogRepository(application);
    }

    public LiveData<ApiResult<List<PerkaraReminderLogGroup>>> getPerkaraGroupsResult() {
        return perkaraGroupsResult;
    }

    public LiveData<ReminderLogSummary> getSummaryResult() {
        return summaryResult;
    }

    public LiveData<Boolean> getIsPaginating() {
        return isPaginating;
    }

    public LiveData<String> getPaginationError() {
        return paginationError;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getCurrentSearch() {
        return currentSearch;
    }

    public String getCurrentTahap() {
        return currentTahap;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Memuat halaman pertama log reminder (status dan search).
     */
    public void loadFirstPage(String status, String search) {
        loadFirstPage(status, search, null);
    }

    /**
     * Memuat halaman pertama log reminder dengan filter lengkap.
     */
    public void loadFirstPage(String status, String search, String tahap) {
        this.currentStatus = (status != null && !status.isEmpty()) ? status : "all";
        this.currentSearch = (search != null) ? search.trim() : "";
        this.currentTahap = (tahap != null && !tahap.isEmpty() && !tahap.equalsIgnoreCase("all")) ? tahap.trim() : null;

        this.currentPage = 1;
        this.lastPage = 1;
        this.isLastPage = false;
        this.isLoading = true;

        this.perkaraGroupMap.clear();
        this.existingLogIds.clear();

        perkaraGroupsResult.setValue(ApiResult.loading());

        fetchPage(1, false);
    }

    /**
     * Memuat halaman berikutnya saat scroll mencapai akhir daftar.
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
     * Muat ulang data sesuai filter aktif.
     */
    public void refresh() {
        loadFirstPage(currentStatus, currentSearch, currentTahap);
    }

    /**
     * Ubah filter status ("all", "selesai", "aktif", "overdue").
     */
    public void setStatusFilter(String status) {
        loadFirstPage(status, currentSearch, currentTahap);
    }

    /**
     * Lakukan pencarian (nomor perkara, tersangka, jaksa, jenis reminder).
     */
    public void setSearchQuery(String search) {
        loadFirstPage(currentStatus, search, currentTahap);
    }

    /**
     * Ubah filter tahap perkara.
     */
    public void setTahapFilter(String tahap) {
        loadFirstPage(currentStatus, currentSearch, tahap);
    }

    private void fetchPage(int pageToFetch, boolean isPaginationCall) {
        MutableLiveData<ApiResult<ReminderLogResponse>> pageResult = new MutableLiveData<>();
        pageResult.observeForever(result -> {
            if (result == null || result.isLoading()) {
                return;
            }

            isLoading = false;
            if (isPaginationCall) {
                isPaginating.setValue(false);
            }

            if (result.isSuccess() && result.getData() != null) {
                ReminderLogResponse responseBody = result.getData();

                // Simpan ringkasan summary KPI jika ada
                if (responseBody.getSummary() != null) {
                    summaryResult.setValue(responseBody.getSummary());
                }

                ReminderLogPagination pageData = responseBody.getData();
                if (pageData != null) {
                    currentPage = pageData.getCurrentPage() > 0 ? pageData.getCurrentPage() : pageToFetch;
                    lastPage = pageData.getLastPage() > 0 ? pageData.getLastPage() : 1;
                    isLastPage = currentPage >= lastPage;

                    List<ReminderLog> newItems = pageData.getData();
                    if (newItems != null) {
                        for (ReminderLog item : newItems) {
                            if (item != null && !existingLogIds.contains(item.getId())) {
                                existingLogIds.add(item.getId());

                                // Tentukan kunci pengelompokan berdasarkan Perkara ID / Nomor Perkara
                                int pId = item.getPerkaraId();
                                String nomorPerkara = null;
                                Perkara pObj = item.getPerkara();
                                if (pObj != null) {
                                    if (pId <= 0) pId = pObj.getId();
                                    nomorPerkara = pObj.getNomorPerkara();
                                }

                                String groupKey = (nomorPerkara != null && !nomorPerkara.trim().isEmpty())
                                        ? nomorPerkara.trim().toLowerCase()
                                        : (pId > 0 ? ("pid_" + pId) : ("log_" + item.getId()));

                                PerkaraReminderLogGroup group = perkaraGroupMap.get(groupKey);
                                if (group == null) {
                                    group = new PerkaraReminderLogGroup(pId, nomorPerkara, pObj);
                                    perkaraGroupMap.put(groupKey, group);
                                } else {
                                    if (pObj != null && group.getPerkara() == null) {
                                        group.setPerkara(pObj);
                                    }
                                }
                                group.addReminderLog(item);
                            }
                        }
                    }
                }

                perkaraGroupsResult.setValue(ApiResult.success(new ArrayList<>(perkaraGroupMap.values())));
            } else if (result.isError()) {
                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat log reminder";
                if (isPaginationCall) {
                    paginationError.setValue(errorMsg);
                } else {
                    perkaraGroupsResult.setValue(ApiResult.error(errorMsg));
                }
            }
        });

        repository.getReminderLogs(currentStatus, currentSearch, currentTahap, pageToFetch, PER_PAGE, pageResult);
    }
}
