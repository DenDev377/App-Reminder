package com.kejaksaan.reminder;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.ChangePasswordRequest;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.model.UpdatePhoneRequest;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.viewmodel.PerkaraViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileActivity — halaman profil pengguna.
 *
 * Data dari GET /api/user ditampilkan di:
 *  - KTA overlay  : foto, nama, jabatan, NIP/NRP, pangkat/golongan
 *  - Kata Sandi   : subtitle = email akun; klik → dialog ganti password
 *  - Nomor HP     : subtitle = nomor_hp; klik → dialog update nomor HP
 */
public class ProfileActivity extends AppCompatActivity {

    // ─── KTA overlay ────────────────────────────────────────────────────────────
    private ImageView imgProfileKta;
    private TextView  tvKtaNama;
    private TextView  tvKtaJabatan;
    private TextView  tvKtaNip;
    private TextView  tvKtaPangkat;

    // ─── Settings subtitles ──────────────────────────────────────────────────────
    private TextView tvInfoEmail;    // subtitle Kata Sandi
    private TextView tvInfoNomorHp;  // subtitle Nomor HP

    private PerkaraViewModel perkaraViewModel;

    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> insets
        );

        initViews();
        setupListeners();
        observeAndLoad();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initViews() {
        imgProfileKta = findViewById(R.id.imgProfileKta);
        tvKtaNama     = findViewById(R.id.tvKtaNama);
        tvKtaJabatan  = findViewById(R.id.tvKtaJabatan);
        tvKtaNip      = findViewById(R.id.tvKtaNip);
        tvKtaPangkat  = findViewById(R.id.tvKtaPangkat);
        tvInfoEmail   = findViewById(R.id.tvInfoEmail);
        tvInfoNomorHp = findViewById(R.id.tvInfoNomorHp);
    }

    // ─── Listeners ───────────────────────────────────────────────────────────────

    private void setupListeners() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // ── Kata Sandi → tampilkan dialog ganti password ──────────────────────
        View menuKataSandi = findViewById(R.id.menuKataSandi);
        if (menuKataSandi != null) {
            menuKataSandi.setOnClickListener(v -> showDialogKataSandi());
        }

        // ── Nomor HP → tampilkan dialog update HP ─────────────────────────────
        View menuNomorHp = findViewById(R.id.menuNomorHp);
        if (menuNomorHp != null) {
            menuNomorHp.setOnClickListener(v -> showDialogNomorHp());
        }

        // ── Menu lainnya (placeholder) ────────────────────────────────────────
        View menuTentangAplikasi = findViewById(R.id.menuTentangAplikasi);
        if (menuTentangAplikasi != null) {
            menuTentangAplikasi.setOnClickListener(v ->
                Toast.makeText(this, "SIPETA v1.0", Toast.LENGTH_SHORT).show()
            );
        }
        View menuPrivacyPolicy = findViewById(R.id.menuPrivacyPolicy);
        if (menuPrivacyPolicy != null) {
            menuPrivacyPolicy.setOnClickListener(v ->
                Toast.makeText(this, "Privacy Policy segera hadir", Toast.LENGTH_SHORT).show()
            );
        }
        View menuUndangTeman = findViewById(R.id.menuUndangTeman);
        if (menuUndangTeman != null) {
            menuUndangTeman.setOnClickListener(v ->
                Toast.makeText(this, "Fitur Undang Teman segera hadir", Toast.LENGTH_SHORT).show()
            );
        }
        View menuFaq = findViewById(R.id.menuFaq);
        if (menuFaq != null) {
            menuFaq.setOnClickListener(v ->
                Toast.makeText(this, "FAQ segera hadir", Toast.LENGTH_SHORT).show()
            );
        }
    }

    // ─── Dialog: Ganti Kata Sandi ────────────────────────────────────────────────

    private void showDialogKataSandi() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kata_sandi, null);

        EditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNew     = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);
        TextView tvError   = dialogView.findViewById(R.id.tvPasswordError);

        // Toggle visibility password
        setupPasswordToggle(dialogView.findViewById(R.id.btnToggleCurrent), etCurrent);
        setupPasswordToggle(dialogView.findViewById(R.id.btnToggleNew),     etNew);
        setupPasswordToggle(dialogView.findViewById(R.id.btnToggleConfirm), etConfirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Simpan", null)  // null dulu supaya bisa override
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String current = etCurrent.getText().toString().trim();
                String newPass = etNew.getText().toString().trim();
                String confirm = etConfirm.getText().toString().trim();

                // Validasi
                if (current.isEmpty()) {
                    showError(tvError, "Kata sandi saat ini tidak boleh kosong");
                    return;
                }
                if (newPass.length() < 8) {
                    showError(tvError, "Kata sandi baru minimal 8 karakter");
                    return;
                }
                if (!newPass.equals(confirm)) {
                    showError(tvError, "Konfirmasi kata sandi tidak cocok");
                    return;
                }
                tvError.setVisibility(View.GONE);

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Menyimpan...");

                callChangePassword(current, newPass, confirm, dialog, tvError);
            });
        });

        dialog.show();
    }

    private void callChangePassword(String current, String newPass, String confirm,
                                    AlertDialog dialog, TextView tvError) {
        ChangePasswordRequest req = new ChangePasswordRequest(current, newPass, confirm);

        RetrofitClient.getApiService(this)
                .changePassword(req)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        runOnUiThread(() -> {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().isSuccess()) {
                                Toast.makeText(ProfileActivity.this,
                                        "Kata sandi berhasil diubah", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                String msg = (response.body() != null && response.body().getMessage() != null)
                                        ? response.body().getMessage()
                                        : "Gagal mengubah kata sandi (HTTP " + response.code() + ")";
                                showError(tvError, msg);
                                resetPositiveButton(dialog, "Simpan");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            showError(tvError, "Tidak dapat terhubung ke server");
                            resetPositiveButton(dialog, "Simpan");
                        });
                    }
                });
    }

    // ─── Dialog: Update Nomor HP ─────────────────────────────────────────────────

    private void showDialogNomorHp() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nomor_hp, null);

        EditText etHp    = dialogView.findViewById(R.id.etNomorHp);
        TextView tvError = dialogView.findViewById(R.id.tvPhoneError);

        // Prefill nomor HP saat ini jika sudah ada
        if (tvInfoNomorHp != null) {
            String current = tvInfoNomorHp.getText().toString();
            if (!current.equals("-") && !current.equals("Belum diatur")) {
                etHp.setText(current);
                etHp.setSelection(current.length());
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String hp = etHp.getText().toString().trim();

                if (hp.isEmpty()) {
                    showError(tvError, "Nomor HP tidak boleh kosong");
                    return;
                }
                if (hp.length() < 9 || hp.length() > 15) {
                    showError(tvError, "Format nomor HP tidak valid");
                    return;
                }
                tvError.setVisibility(View.GONE);

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Menyimpan...");

                callUpdatePhone(hp, dialog, tvError);
            });
        });

        dialog.show();
    }

    private void callUpdatePhone(String hp, AlertDialog dialog, TextView tvError) {
        UpdatePhoneRequest req = new UpdatePhoneRequest(hp);

        RetrofitClient.getApiService(this)
                .updatePhone(req)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        runOnUiThread(() -> {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().isSuccess()) {
                                // Update subtitle langsung tanpa reload
                                if (tvInfoNomorHp != null) tvInfoNomorHp.setText(hp);
                                Toast.makeText(ProfileActivity.this,
                                        "Nomor HP berhasil diperbarui", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                String msg = (response.body() != null && response.body().getMessage() != null)
                                        ? response.body().getMessage()
                                        : "Gagal memperbarui nomor HP (HTTP " + response.code() + ")";
                                showError(tvError, msg);
                                resetPositiveButton(dialog, "Simpan");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            showError(tvError, "Tidak dapat terhubung ke server");
                            resetPositiveButton(dialog, "Simpan");
                        });
                    }
                });
    }

    // ─── ViewModel & Data ────────────────────────────────────────────────────────

    private void observeAndLoad() {
        perkaraViewModel = new ViewModelProvider(this).get(PerkaraViewModel.class);

        perkaraViewModel.getUserProfileResult().observe(this, result -> {
            if (result == null || result.isLoading()) return;
            if (result.isSuccess() && result.getData() != null) {
                bindUserProfile(result.getData());
            } else if (result.isError()) {
                Toast.makeText(this, "Gagal memuat profil: " + result.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        perkaraViewModel.loadUserProfile();
    }

    // ─── Bind UI ─────────────────────────────────────────────────────────────────

    private void bindUserProfile(User user) {
        if (tvKtaNama != null)
            tvKtaNama.setText(notEmpty(user.getName(), "-"));

        if (tvKtaJabatan != null) {
            String j = notEmpty(user.getJabatan(), null);
            tvKtaJabatan.setText(j != null ? j : capitalize(user.getRole()));
        }

        if (tvKtaNip != null)
            tvKtaNip.setText(buildNipNrp(user));

        if (tvKtaPangkat != null) {
            String p = notEmpty(user.getPangkatGolongan(), null);
            tvKtaPangkat.setText(p != null ? "Pangkat: " + p : "Pangkat: -");
        }

        if (imgProfileKta != null) {
            String url = user.getResolvedPhotoUrl();
            if (url != null) {
                Glide.with(this).load(url)
                     .placeholder(R.drawable.logo_kejaksaan)
                     .error(R.drawable.logo_kejaksaan)
                     .centerCrop().into(imgProfileKta);
            } else {
                imgProfileKta.setImageResource(R.drawable.logo_kejaksaan);
            }
        }

        if (tvInfoEmail != null)
            tvInfoEmail.setText(notEmpty(user.getEmail(), "-"));

        if (tvInfoNomorHp != null) {
            String hp = notEmpty(user.getNomorHp(), null);
            tvInfoNomorHp.setText(hp != null ? hp : "Belum diatur");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void showError(TextView tv, String msg) {
        tv.setText(msg);
        tv.setVisibility(View.VISIBLE);
    }

    private void resetPositiveButton(AlertDialog dialog, String label) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(label);
    }

    /** Toggle show/hide password pada EditText. */
    private void setupPasswordToggle(ImageView toggleBtn, EditText editText) {
        if (toggleBtn == null || editText == null) return;
        toggleBtn.setTag(false); // false = hidden
        toggleBtn.setOnClickListener(v -> {
            boolean isVisible = (boolean) toggleBtn.getTag();
            if (isVisible) {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggleBtn.setImageResource(R.drawable.ic_eye_off);
            } else {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggleBtn.setImageResource(R.drawable.ic_eye);
            }
            toggleBtn.setTag(!isVisible);
            editText.setSelection(editText.getText().length());
        });
    }

    private String notEmpty(String v, String fallback) {
        return (v != null && !v.isEmpty()) ? v : fallback;
    }

    private String buildNipNrp(User user) {
        boolean hasNip = user.getNip() != null && !user.getNip().isEmpty();
        boolean hasNrp = user.getNrp() != null && !user.getNrp().isEmpty();
        if (hasNip && hasNrp) return "NIP: " + user.getNip() + "  ·  NRP: " + user.getNrp();
        if (hasNip)           return "NIP: " + user.getNip();
        if (hasNrp)           return "NRP: " + user.getNrp();
        return "NIP/NRP: -";
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (String word : text.replace("_", " ").split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
