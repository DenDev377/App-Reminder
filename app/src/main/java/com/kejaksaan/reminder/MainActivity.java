package com.kejaksaan.reminder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.util.ApiResult;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.AuthViewModel;

/**
 * MainActivity — Layar Login SIPETA Kejaksaan Negeri Ogan Komering Ulu.
 *
 * Menggunakan AuthViewModel (MVVM pattern) untuk:
 * - Melakukan autentikasi via API
 * - Menyimpan token & sesi pengguna
 * - Fitur "Ingat Saya" (Remember Me) untuk kredensial
 * - Validasi input dengan indikator warna visual per-field
 * - Tombol clear username dan password visibility toggle yang mulus
 * - Bantuan Helpdesk akun kedinasan
 */
public class MainActivity extends AppCompatActivity {

    private EditText     etUsername;
    private EditText     etPassword;
    private Button       btnLogin;
    private ProgressBar  progressBar;
    private ImageView    ivPasswordToggle;
    private ImageView    ivUsernameClear;
    private ImageView    ivUsernameIcon;
    private ImageView    ivPasswordIcon;
    private LinearLayout containerUsername;
    private LinearLayout containerPassword;

    private CheckBox     cbRememberMe;
    private TextView     tvHelp;

    private LinearLayout layoutErrorAlert;
    private TextView     tvErrorMessage;
    private ImageView    ivCloseError;

    private boolean isPasswordVisible = false;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        setupWindowInsets();
        bindViews();
        setupInputInteractions();
        loadRememberedUser();

        // Inisialisasi ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Jika sudah login, langsung ke dashboard
        if (authViewModel.isLoggedIn()) {
            navigateAfterLogin(authViewModel.getSavedRole());
            return;
        }

        observeLoginResult();

        btnLogin.setOnClickListener(v -> performLogin());
        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());
        ivUsernameClear.setOnClickListener(v -> {
            etUsername.setText("");
            etUsername.requestFocus();
        });
        ivCloseError.setOnClickListener(v -> hideError());
        tvHelp.setOnClickListener(v -> showHelpDialog());
    }

    private void bindViews() {
        etUsername        = findViewById(R.id.etUsername);
        etPassword        = findViewById(R.id.etPassword);
        btnLogin          = findViewById(R.id.btnLogin);
        progressBar       = findViewById(R.id.progressBar);
        ivPasswordToggle  = findViewById(R.id.ivPasswordToggle);
        ivUsernameClear   = findViewById(R.id.ivUsernameClear);
        ivUsernameIcon    = findViewById(R.id.ivUsernameIcon);
        ivPasswordIcon    = findViewById(R.id.ivPasswordIcon);
        containerUsername = findViewById(R.id.containerUsername);
        containerPassword = findViewById(R.id.containerPassword);

        cbRememberMe      = findViewById(R.id.cbRememberMe);
        tvHelp            = findViewById(R.id.tvHelp);

        layoutErrorAlert  = findViewById(R.id.layoutErrorAlert);
        tvErrorMessage    = findViewById(R.id.tvErrorMessage);
        ivCloseError      = findViewById(R.id.ivCloseError);

        // Sentuh area luar untuk menutup keyboard
        View mainRoot = findViewById(R.id.mainRootLayout);
        if (mainRoot != null) {
            mainRoot.setOnClickListener(v -> hideKeyboard());
        }
        View cardLogin = findViewById(R.id.cardLogin);
        if (cardLogin != null) {
            cardLogin.setOnClickListener(v -> hideKeyboard());
        }
    }

    private void setupWindowInsets() {
        // Terapkan insets ke scrollview agar header batik tetap menempel indah di bawah status bar
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.nestedScrollView),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(0, systemBars.top, 0, systemBars.bottom);
                    return insets;
                }
        );
    }

    private void loadRememberedUser() {
        boolean isRemembered = sessionManager.isRememberUser();
        cbRememberMe.setChecked(isRemembered);
        if (isRemembered) {
            String savedEmail = sessionManager.getRememberedEmail();
            if (savedEmail != null && !savedEmail.trim().isEmpty()) {
                etUsername.setText(savedEmail.trim());
                ivUsernameClear.setVisibility(View.VISIBLE);
                etPassword.requestFocus();
            }
        }
    }

    private void setupInputInteractions() {
        // Dinamis ubah state fokus dan warna ikon input
        etUsername.setOnFocusChangeListener((v, hasFocus) -> {
            containerUsername.setActivated(hasFocus);
            if (!containerUsername.isSelected() && ivUsernameIcon != null) {
                int iconColor = hasFocus ? Color.parseColor("#00897B") : Color.parseColor("#94A3B8");
                ivUsernameIcon.setColorFilter(iconColor);
            }
            if (hasFocus) {
                hideError();
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            containerPassword.setActivated(hasFocus);
            if (!containerPassword.isSelected() && ivPasswordIcon != null) {
                int iconColor = hasFocus ? Color.parseColor("#00897B") : Color.parseColor("#94A3B8");
                ivPasswordIcon.setColorFilter(iconColor);
            }
            if (hasFocus) {
                hideError();
            }
        });

        // Kontrol tombol clear teks username dan auto-dismiss error saat mengetik
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivUsernameClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                hideError();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hideError();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Tekan 'Done / Enter' pada keyboard virtual langsung menjalankan login
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin();
                return true;
            }
            return false;
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivPasswordToggle.setImageResource(R.drawable.ic_eye);
            ivPasswordToggle.setColorFilter(Color.parseColor("#00897B"));
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_off);
            ivPasswordToggle.setColorFilter(Color.parseColor("#94A3B8"));
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void observeLoginResult() {
        authViewModel.getLoginResult().observe(this, this::handleLoginState);
    }

    private void handleLoginState(ApiResult<User> result) {
        if (result == null) return;

        if (result.isLoading()) {
            setLoadingState(true);
        } else if (result.isSuccess()) {
            setLoadingState(false);
            hideError();

            // Simpan atau bersihkan preferensi "Ingat Saya"
            String currentEmail = etUsername.getText().toString().trim();
            sessionManager.setRememberUser(cbRememberMe.isChecked(), currentEmail);

            User user = result.getData();
            String greeting = "Selamat datang, " + (user != null ? user.getName() : "");
            Toast.makeText(this, greeting, Toast.LENGTH_SHORT).show();
            navigateAfterLogin(user != null ? user.getRole() : "");
        } else {
            setLoadingState(false);
            String rawMessage = result.getMessage();
            String displayMsg = (rawMessage != null && !rawMessage.trim().isEmpty())
                    ? rawMessage.trim()
                    : "Email atau kata sandi tidak sesuai. Silakan coba lagi.";

            setFieldError(true, true);
            showError(displayMsg);
        }
    }

    private void performLogin() {
        hideKeyboard();
        String email    = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            showError("Silakan masukkan Email atau Username Anda.");
            setFieldError(true, false);
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Silakan masukkan Kata Sandi Anda.");
            setFieldError(false, true);
            etPassword.requestFocus();
            return;
        }

        hideError();
        authViewModel.login(email, password);
    }

    private void setFieldError(boolean isUsernameError, boolean isPasswordError) {
        if (containerUsername != null) {
            containerUsername.setSelected(isUsernameError);
            if (ivUsernameIcon != null) {
                ivUsernameIcon.setColorFilter(isUsernameError
                        ? Color.parseColor("#EF4444")
                        : (etUsername.hasFocus() ? Color.parseColor("#00897B") : Color.parseColor("#94A3B8")));
            }
        }
        if (containerPassword != null) {
            containerPassword.setSelected(isPasswordError);
            if (ivPasswordIcon != null) {
                ivPasswordIcon.setColorFilter(isPasswordError
                        ? Color.parseColor("#EF4444")
                        : (etPassword.hasFocus() ? Color.parseColor("#00897B") : Color.parseColor("#94A3B8")));
            }
        }
    }

    private void showError(String message) {
        if (layoutErrorAlert != null && tvErrorMessage != null) {
            tvErrorMessage.setText(message);
            layoutErrorAlert.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void hideError() {
        if (layoutErrorAlert != null && layoutErrorAlert.getVisibility() == View.VISIBLE) {
            layoutErrorAlert.setVisibility(View.GONE);
        }
        setFieldError(false, false);
    }

    private void setLoadingState(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "" : "MASUK KE SISTEM");
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        etUsername.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        ivPasswordToggle.setEnabled(!loading);
        ivUsernameClear.setEnabled(!loading);
        cbRememberMe.setEnabled(!loading);
    }

    private void showHelpDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Bantuan Masuk Akun")
                .setMessage("Jika Anda lupa kata sandi atau mengalami kendala masuk ke sistem SIPETA:\n\n"
                        + "1. Hubungi Administrator Seksi Tindak Pidana Umum (Pidum)\n"
                        + "2. Hubungi Tim IT / Daskrimti Kejari Ogan Komering Ulu\n"
                        + "3. Email resmi: kn.oku@kejaksaan.go.id\n\n"
                        + "Pastikan akun dinas Anda telah diaktifkan oleh Administrator.")
                .setPositiveButton("Mengerti", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /**
     * Navigasi ke layar dashboard setelah login.
     * Jika ada pending_perkara_id dari notifikasi push FCM saat user belum login,
     * otomatis teruskan pengguna langsung ke DetailPerkaraActivity.
     */
    private void navigateAfterLogin(String role) {
        int pendingPerkaraId = getIntent().getIntExtra("pending_perkara_id", -1);
        String pendingNomorPerkara = getIntent().getStringExtra("pending_nomor_perkara");

        Intent dashboardIntent = new Intent(this, DashboardActivity.class);
        startActivity(dashboardIntent);

        if (pendingPerkaraId > 0) {
            Intent detailIntent = new Intent(this, DetailPerkaraActivity.class);
            detailIntent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, pendingPerkaraId);
            if (pendingNomorPerkara != null) {
                detailIntent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, pendingNomorPerkara);
            }
            startActivity(detailIntent);
        }

        finish();
    }
}