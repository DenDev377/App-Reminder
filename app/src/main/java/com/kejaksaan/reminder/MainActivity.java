package com.kejaksaan.reminder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.util.ApiResult;
import com.kejaksaan.reminder.viewmodel.AuthViewModel;

/**
 * MainActivity — Layar Login SIPETA.
 *
 * Menggunakan AuthViewModel (MVVM pattern) untuk:
 * - Melakukan login via API
 * - Menyimpan token & info user otomatis
 * - Menampilkan state loading / sukses / error via LiveData
 *
 * TODO: Setelah login berhasil, navigasikan ke DashboardActivity berdasarkan role.
 */
public class MainActivity extends AppCompatActivity {

    private EditText    etUsername;
    private EditText    etPassword;
    private Button      btnLogin;
    private ProgressBar progressBar;
    private ImageView   ivPasswordToggle;

    private boolean isPasswordVisible = false;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupWindowInsets();
        bindViews();

        // Inisialisasi ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Jika sudah login, langsung ke dashboard
        if (authViewModel.isLoggedIn()) {
            navigateToDashboard(authViewModel.getSavedRole());
            return;
        }

        observeLoginResult();

        btnLogin.setOnClickListener(v -> performLogin());

        // Password visibility toggle logic
        ivPasswordToggle.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                // Show Password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye);
            } else {
                // Hide Password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_off);
            }
            // Move cursor to the end
            etPassword.setSelection(etPassword.getText().length());
            // Preserve typeface/font after changing inputType (Android bug workaround)
            etPassword.setTypeface(android.graphics.Typeface.DEFAULT);
        });
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private void bindViews() {
        etUsername  = findViewById(R.id.etUsername);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle);
        
        // progressBar optional — tambahkan di layout jika belum ada
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.imgLogo),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            v.getPaddingLeft(),
                            systemBars.top + v.getPaddingTop(),
                            v.getPaddingRight(),
                            v.getPaddingBottom()
                    );
                    return insets;
                }
        );
    }

    private void observeLoginResult() {
        authViewModel.getLoginResult().observe(this, result -> {
            handleLoginState(result);
        });
    }

    private void handleLoginState(ApiResult<User> result) {
        if (result.isLoading()) {
            setLoadingState(true);

        } else if (result.isSuccess()) {
            setLoadingState(false);
            User user = result.getData();
            String greeting = "Selamat datang, " + (user != null ? user.getName() : "");
            Toast.makeText(this, greeting, Toast.LENGTH_SHORT).show();
            navigateToDashboard(user != null ? user.getRole() : "");

        } else {
            setLoadingState(false);
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performLogin() {
        String email    = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Mohon isi Email dan Password", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.login(email, password);
    }

    private void setLoadingState(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Loading..." : "LOGIN");
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Navigasi ke layar dashboard berdasarkan role user.
     * TODO: Ganti Intent sesuai Activity Dashboard Anda.
     */
    private void navigateToDashboard(String role) {
        android.content.Intent intent = new android.content.Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}