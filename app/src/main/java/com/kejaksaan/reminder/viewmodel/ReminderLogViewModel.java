package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

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
 * Melakukan agregasi metrik KPI dan filtering status pada level Perkara.
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
     * Menyaring data perkara secara instan di memori jika data sudah terisi.
     */
    public void setStatusFilter(String status) {
        this.currentStatus = (status != null && !status.isEmpty()) ? status : "all";
        if (perkaraGroupMap.isEmpty()) {
            loadFirstPage(this.currentStatus, currentSearch, currentTahap);
        } else {
            applyFilterAndPost();
        }
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

    private String getPerkaraGroupKey(int pId, String nomorPerkara) {
        if (pId > 0) {
            return "pid_" + pId;
        }
        if (nomorPerkara != null && !nomorPerkara.trim().isEmpty()) {
            return "no_" + nomorPerkara.trim().toLowerCase();
        }
        return "obj_" + System.currentTimeMillis();
    }

    private void fetchPage(int pageToFetch, boolean isPaginationCall) {
        // Ambil data perkara induk resmi pada halaman 1 untuk menjamin semua perkara aktif
        // (termasuk yang berada di Tahap II dan P-24) masuk ke dalam grouping
        if (pageToFetch == 1) {
            MutableLiveData<ApiResult<List<Perkara>>> perkarasResult = new MutableLiveData<>();
            Observer<ApiResult<List<Perkara>>> perkaraObserver = new Observer<ApiResult<List<Perkara>>>() {
                @Override
                public void onChanged(ApiResult<List<Perkara>> pResult) {
                    if (pResult == null || pResult.isLoading()) {
                        return;
                    }
                    perkarasResult.removeObserver(this);
                    if (pResult.isSuccess() && pResult.getData() != null) {
                        for (Perkara p : pResult.getData()) {
                            if (p != null) {
                                String key = getPerkaraGroupKey(p.getId(), p.getNomorPerkara());
                                PerkaraReminderLogGroup group = perkaraGroupMap.get(key);
                                if (group == null) {
                                    group = new PerkaraReminderLogGroup(p.getId(), p.getNomorPerkara(), p);
                                    perkaraGroupMap.put(key, group);
                                } else {
                                    group.setPerkara(p);
                                }
                            }
                        }
                        updateSummaryAndFilter();
                    }
                }
            };
            perkarasResult.observeForever(perkaraObserver);
            repository.getPerkaras(1, currentSearch, perkarasResult);
        }

        // Ambil data log reminder SLA (selalu status=null di backend agar tidak memutus rantai riwayat)
        MutableLiveData<ApiResult<ReminderLogResponse>> pageResult = new MutableLiveData<>();
        Observer<ApiResult<ReminderLogResponse>> logObserver = new Observer<ApiResult<ReminderLogResponse>>() {
            @Override
            public void onChanged(ApiResult<ReminderLogResponse> result) {
                if (result == null || result.isLoading()) {
                    return;
                }
                pageResult.removeObserver(this);

                isLoading = false;
                if (isPaginationCall) {
                    isPaginating.setValue(false);
                }

                if (result.isSuccess() && result.getData() != null) {
                    ReminderLogResponse responseBody = result.getData();
                    ReminderLogPagination pageData = responseBody.getData();

                    if (pageData != null) {
                        currentPage = pageData.getCurrentPage() > 0 ? pageData.getCurrentPage() : pageToFetch;
                        lastPage = pageData.getLastPage() > 0 ? pageData.getLastPage() : 1;
                        isLastPage = currentPage >= lastPage;

                        List<ReminderLog> newItems = pageData.getData();
                        if (newItems != null && !newItems.isEmpty()) {
                            for (ReminderLog item : newItems) {
                                if (item != null && !existingLogIds.contains(item.getId())) {
                                    existingLogIds.add(item.getId());

                                    int pId = item.getPerkaraId();
                                    String nomorPerkara = null;
                                    Perkara pObj = item.getPerkara();
                                    if (pObj != null) {
                                        if (pId <= 0) pId = pObj.getId();
                                        nomorPerkara = pObj.getNomorPerkara();
                                    }

                                    String groupKey = getPerkaraGroupKey(pId, nomorPerkara);
                                    PerkaraReminderLogGroup group = perkaraGroupMap.get(groupKey);

                                    // Cek fallback jika tersimpan dengan nomor perkara
                                    if (group == null && nomorPerkara != null && !nomorPerkara.trim().isEmpty()) {
                                        group = perkaraGroupMap.get("no_" + nomorPerkara.trim().toLowerCase());
                                    }

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
                        } else {
                            isLastPage = true;
                        }
                    } else {
                        isLastPage = true;
                    }

                    updateSummaryAndFilter();
                } else if (result.isError()) {
                    String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat log reminder";
                    if (isPaginationCall) {
                        paginationError.setValue(errorMsg);
                    } else {
                        if (perkaraGroupMap.isEmpty()) {
                            perkaraGroupsResult.setValue(ApiResult.error(errorMsg));
                        } else {
                            updateSummaryAndFilter();
                        }
                    }
                }
            }
        };
        pageResult.observeForever(logObserver);

        // Jangan batasi status pada backend reminder_logs agar riwayat lengkap tidak terpotong
        repository.getReminderLogs(null, currentSearch, currentTahap, pageToFetch, PER_PAGE, pageResult);
    }

    /**
     * Menghitung metrik KPI ringkasan pada level perkara (Perkara-level metrics)
     * dan memposting ke summaryResult.
     */
    private void updateSummaryAndFilter() {
        int totalPerkara = perkaraGroupMap.size();
        int selesaiPerkara = 0;
        int overduePerkara = 0;
        int aktifPerkara = 0;

        for (PerkaraReminderLogGroup group : perkaraGroupMap.values()) {
            if (group.isPerkaraFinished()) {
                selesaiPerkara++;
            } else if (group.hasOverdue()) {
                overduePerkara++;
            } else {
                aktifPerkara++;
            }
        }

        summaryResult.setValue(new ReminderLogSummary(totalPerkara, selesaiPerkara, aktifPerkara, overduePerkara));
        applyFilterAndPost();
    }

    /**
     * Menerapkan filter status perkara ("all", "aktif", "overdue", "selesai")
     * dan memposting daftar hasil ke perkaraGroupsResult.
     */
    private void applyFilterAndPost() {
        List<PerkaraReminderLogGroup> filtered = new ArrayList<>();
        String status = currentStatus != null ? currentStatus.trim().toLowerCase() : "all";

        for (PerkaraReminderLogGroup group : perkaraGroupMap.values()) {
            if ("all".equalsIgnoreCase(status) || "semua".equalsIgnoreCase(status)) {
                filtered.add(group);
            } else if ("selesai".equalsIgnoreCase(status)) {
                if (group.isPerkaraFinished()) {
                    filtered.add(group);
                }
            } else if ("overdue".equalsIgnoreCase(status)) {
                if (group.hasOverdue()) {
                    filtered.add(group);
                }
            } else if ("aktif".equalsIgnoreCase(status)) {
                // Perkara aktif adalah perkara yang masih berjalan (belum selesai dan tidak overdue)
                if (!group.isPerkaraFinished() && !group.hasOverdue()) {
                    filtered.add(group);
                }
            }
        }

        perkaraGroupsResult.setValue(ApiResult.success(filtered));
    }
}
