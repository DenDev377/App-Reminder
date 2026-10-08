package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.repository.PerkaraRepository;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.List;

/**
 * PerkaraViewModel — ViewModel untuk layar Daftar Perkara & Profil.
 *
 * Cara observasi di Activity/Fragment:
 * <pre>
 *   PerkaraViewModel vm = new ViewModelProvider(this).get(PerkaraViewModel.class);
 *
 *   // Observe daftar perkara
 *   vm.getPerkarasResult().observe(this, result -> {
 *       if (result.isLoading()) { showShimmer(); }
 *       else if (result.isSuccess()) { adapter.submitList(result.getData()); }
 *       else { showError(result.getMessage()); }
 *   });
 *
 *   // Observe profil user
 *   vm.getUserProfileResult().observe(this, result -> {
 *       if (result.isSuccess()) { bindProfile(result.getData()); }
 *   });
 *
 *   vm.loadPerkaras();
 *   vm.loadUserProfile();
 * </pre>
 */
public class PerkaraViewModel extends AndroidViewModel {

    private final PerkaraRepository perkaraRepository;

    private final MutableLiveData<ApiResult<List<Perkara>>> perkarasResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<User>>          userProfileResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<List<com.kejaksaan.reminder.model.Reminder>>> slaRemindersResult = new MutableLiveData<>();

    /** Halaman paginasi saat ini */
    private int currentPage = 1;

    public PerkaraViewModel(@NonNull Application application) {
        super(application);
        perkaraRepository = new PerkaraRepository(application);
    }

    // ─── Exposed LiveData ────────────────────────────────────────────────────────

    public LiveData<ApiResult<List<Perkara>>> getPerkarasResult() {
        return perkarasResult;
    }

    public LiveData<ApiResult<User>> getUserProfileResult() {
        return userProfileResult;
    }

    public LiveData<ApiResult<List<com.kejaksaan.reminder.model.Reminder>>> getSlaRemindersResult() {
        return slaRemindersResult;
    }

    // ─── Actions ─────────────────────────────────────────────────────────────────

    /**
     * Muat daftar perkara dari halaman pertama (reset pagination).
     */
    public void loadPerkaras() {
        currentPage = 1;
        perkaraRepository.getPerkaras(currentPage, perkarasResult);
    }

    /**
     * Muat halaman berikutnya (untuk infinite scroll / load more).
     */
    public void loadNextPage() {
        currentPage++;
        perkaraRepository.getPerkaras(currentPage, perkarasResult);
    }

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
