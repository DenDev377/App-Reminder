package com.kejaksaan.reminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.LoginRequest;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.repository.AuthRepository;
import com.kejaksaan.reminder.util.ApiResult;

/**
 * AuthViewModel — ViewModel untuk layar Login / Autentikasi.
 *
 * Menggunakan AndroidViewModel agar bisa mengakses Application context
 * tanpa menyebabkan memory leak.
 *
 * Cara observasi di Activity:
 * <pre>
 *   AuthViewModel vm = new ViewModelProvider(this).get(AuthViewModel.class);
 *   vm.getLoginResult().observe(this, result -> {
 *       if (result.isLoading()) { showProgress(); }
 *       else if (result.isSuccess()) { navigateToDashboard(); }
 *       else { showError(result.getMessage()); }
 *   });
 *   vm.login("email@example.com", "password");
 * </pre>
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    /** LiveData yang di-observe oleh LoginActivity */
    private final MutableLiveData<ApiResult<User>> loginResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
    }

    // ─── Exposed LiveData ────────────────────────────────────────────────────────

    public LiveData<ApiResult<User>> getLoginResult() {
        return loginResult;
    }

    // ─── Actions ─────────────────────────────────────────────────────────────────

    /**
     * Memulai proses login.
     * Hasilnya akan di-post ke loginResult LiveData.
     */
    public void login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        authRepository.login(request, loginResult);
    }

    /**
     * Logout: hapus sesi & panggil API.
     */
    public void logout() {
        authRepository.logout(getApplication());
    }

    /**
     * Mengecek apakah user sudah login (ada token tersimpan).
     */
    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    /**
     * Mengambil role user yang tersimpan di sesi (offline).
     */
    public String getSavedRole() {
        return authRepository.getSessionManager().fetchUserRole();
    }
}
