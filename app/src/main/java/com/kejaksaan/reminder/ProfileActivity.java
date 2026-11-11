package com.kejaksaan.reminder;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.ChangePasswordRequest;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.model.UpdatePhoneRequest;
import com.kejaksaan.reminder.model.UpdatePhoneResponse;
import com.kejaksaan.reminder.model.UpdateProfileRequest;
import com.kejaksaan.reminder.model.UpdateProfileResponse;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.util.SessionManager;
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
    private TextView tvInfoDataPegawai; // subtitle Data Pegawai (Edit Profil)
    private TextView tvInfoEmail;       // subtitle Kata Sandi
    private TextView tvInfoNomorHp;     // subtitle Nomor HP

    private ImageView ivRefreshIcon;
    private SwipeRefreshLayout swipeRefresh;
    private PerkaraViewModel perkaraViewModel;
    private User currentUser;

    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verifikasi sesi pengguna
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View topActionBar = findViewById(R.id.topActionBar);
        if (topActionBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topActionBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        v.getPaddingLeft(),
                        systemBars.top + 8,
                        v.getPaddingRight(),
                        v.getPaddingBottom()
                );
                return insets;
            });
        }

        initViews();
        setupListeners();
        observeAndLoad();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initViews() {
        imgProfileKta     = findViewById(R.id.imgProfileKta);
        tvKtaNama         = findViewById(R.id.tvKtaNama);
        tvKtaJabatan      = findViewById(R.id.tvKtaJabatan);
        tvKtaNip          = findViewById(R.id.tvKtaNip);
        tvKtaPangkat      = findViewById(R.id.tvKtaPangkat);
        tvInfoDataPegawai = findViewById(R.id.tvInfoDataPegawai);
        tvInfoEmail       = findViewById(R.id.tvInfoEmail);
        tvInfoNomorHp     = findViewById(R.id.tvInfoNomorHp);
        ivRefreshIcon     = findViewById(R.id.ivRefreshIcon);
        swipeRefresh      = findViewById(R.id.swipeRefresh);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    Color.parseColor("#00897B"),
                    Color.parseColor("#DC2626"),
                    Color.parseColor("#D97706")
            );
            swipeRefresh.setOnRefreshListener(() -> {
                triggerRefreshAnimation();
                perkaraViewModel.loadUserProfile();
            });
        }
    }

    private void triggerRefreshAnimation() {
        if (ivRefreshIcon != null) {
            ivRefreshIcon.animate()
                    .rotationBy(360f)
                    .setDuration(600)
                    .withEndAction(() -> ivRefreshIcon.setRotation(0f))
                    .start();
        }
    }

    // ─── Listeners ───────────────────────────────────────────────────────────────

    private void setupListeners() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnRefresh = findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                triggerRefreshAnimation();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                perkaraViewModel.loadUserProfile();
            });
        }

        // ── Data Pegawai / Edit Profil ────────────────────────────────────────
        View menuEditProfil = findViewById(R.id.menuEditProfil);
        if (menuEditProfil != null) {
            menuEditProfil.setOnClickListener(v -> showDialogEditProfil());
        }

        View btnEditKta = findViewById(R.id.btnEditKta);
        if (btnEditKta != null) {
            btnEditKta.setOnClickListener(v -> showDialogEditProfil());
        }

        View cardKta = findViewById(R.id.cardKta);
        if (cardKta != null) {
            cardKta.setOnClickListener(v -> showDialogEditProfil());
        }

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

        // ── Menu lainnya ─────────────────────────────────────────────────────
        View menuTentangAplikasi = findViewById(R.id.menuTentangAplikasi);
        if (menuTentangAplikasi != null) {
            menuTentangAplikasi.setOnClickListener(v -> {
                Intent intent = new Intent(this, TentangAplikasiActivity.class);
                startActivity(intent);
            });
        }
        View menuPrivacyPolicy = findViewById(R.id.menuPrivacyPolicy);
        if (menuPrivacyPolicy != null) {
            menuPrivacyPolicy.setOnClickListener(v -> {
                Intent intent = new Intent(this, PrivacyPolicyActivity.class);
                startActivity(intent);
            });
        }
        View menuFaq = findViewById(R.id.menuFaq);
        if (menuFaq != null) {
            menuFaq.setOnClickListener(v -> {
                Intent intent = new Intent(this, FaqActivity.class);
                startActivity(intent);
            });
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
                if (newPass.equals(current)) {
                    showError(tvError, "Kata sandi baru tidak boleh sama dengan kata sandi saat ini");
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
                                String msg = null;
                                if (response.body() != null && response.body().getMessage() != null) {
                                    msg = response.body().getMessage();
                                } else if (response.errorBody() != null) {
                                    try {
                                        String errStr = response.errorBody().string();
                                        org.json.JSONObject obj = new org.json.JSONObject(errStr);
                                        if (obj.has("message")) {
                                            msg = obj.getString("message");
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                                if (msg == null || msg.trim().isEmpty()) {
                                    msg = "Gagal mengubah kata sandi (HTTP " + response.code() + ")";
                                }
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
                .enqueue(new Callback<UpdatePhoneResponse>() {
                    @Override
                    public void onResponse(Call<UpdatePhoneResponse> call, Response<UpdatePhoneResponse> response) {
                        runOnUiThread(() -> {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().isSuccess()) {
                                // Update subtitle langsung tanpa reload
                                if (tvInfoNomorHp != null) tvInfoNomorHp.setText(hp);

                                // Sinkronkan ke SessionManager jika response menyertakan user
                                if (response.body().getData() != null && response.body().getData().getUser() != null) {
                                    new SessionManager(ProfileActivity.this)
                                            .saveUserInfo(response.body().getData().getUser());
                                }

                                String msg = response.body().getMessage();
                                Toast.makeText(ProfileActivity.this,
                                        (msg != null && !msg.isEmpty()) ? msg : "Nomor HP berhasil diperbarui",
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                String msg = null;
                                if (response.body() != null && response.body().getMessage() != null) {
                                    msg = response.body().getMessage();
                                } else if (response.errorBody() != null) {
                                    try {
                                        String errStr = response.errorBody().string();
                                        org.json.JSONObject obj = new org.json.JSONObject(errStr);
                                        if (obj.has("message")) {
                                            msg = obj.getString("message");
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                                if (msg == null || msg.trim().isEmpty()) {
                                    msg = "Gagal memperbarui nomor HP (HTTP " + response.code() + ")";
                                }
                                showError(tvError, msg);
                                resetPositiveButton(dialog, "Simpan");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<UpdatePhoneResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            showError(tvError, "Tidak dapat terhubung ke server");
                            resetPositiveButton(dialog, "Simpan");
                        });
                    }
                });
    }

    // ─── Dialog: Edit Data Profil (Nama, NIP, Pangkat, Jabatan) ──────────────────

    private void showDialogEditProfil() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profil, null);

        EditText etNama    = dialogView.findViewById(R.id.etProfileNama);
        EditText etNip     = dialogView.findViewById(R.id.etProfileNip);
        EditText etPangkat = dialogView.findViewById(R.id.etProfilePangkat);
        EditText etJabatan = dialogView.findViewById(R.id.etProfileJabatan);
        TextView tvError   = dialogView.findViewById(R.id.tvProfileError);

        // Prefill dengan data saat ini
        SessionManager sessionManager = new SessionManager(this);
        String currName = (currentUser != null && currentUser.getName() != null)
                ? currentUser.getName() : sessionManager.fetchUserName();
        String currNip = (currentUser != null && currentUser.getNip() != null)
                ? currentUser.getNip() : sessionManager.fetchUserNip();
        String currPangkat = (currentUser != null && currentUser.getPangkatGolongan() != null)
                ? currentUser.getPangkatGolongan() : sessionManager.fetchUserPangkat();
        String currJabatan = (currentUser != null && currentUser.getJabatan() != null)
                ? currentUser.getJabatan() : sessionManager.fetchUserJabatan();

        if (currName != null && !currName.isEmpty() && !currName.equals("-")) {
            etNama.setText(currName);
            etNama.setSelection(currName.length());
        }
        if (currNip != null && !currNip.isEmpty() && !currNip.equals("-")) {
            etNip.setText(currNip);
        }
        if (currPangkat != null && !currPangkat.isEmpty() && !currPangkat.equals("-")) {
            etPangkat.setText(currPangkat);
        }
        if (currJabatan != null && !currJabatan.isEmpty() && !currJabatan.equals("-")) {
            etJabatan.setText(currJabatan);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String nama = etNama.getText().toString().trim();
                String nip = etNip.getText().toString().trim();
                String pangkat = etPangkat.getText().toString().trim();
                String jabatan = etJabatan.getText().toString().trim();

                if (nama.isEmpty()) {
                    showError(tvError, "Nama lengkap tidak boleh kosong");
                    return;
                }
                if (nip.isEmpty()) {
                    showError(tvError, "NIP / NRP tidak boleh kosong");
                    return;
                }
                if (pangkat.isEmpty()) {
                    showError(tvError, "Pangkat / Golongan tidak boleh kosong");
                    return;
                }
                if (jabatan.isEmpty()) {
                    showError(tvError, "Jabatan tidak boleh kosong");
                    return;
                }

                tvError.setVisibility(View.GONE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Menyimpan...");

                callUpdateProfile(nama, nip, pangkat, jabatan, dialog, tvError);
            });
        });

        dialog.show();
    }

    private void callUpdateProfile(String nama, String nip, String pangkat, String jabatan,
                                   AlertDialog dialog, TextView tvError) {
        UpdateProfileRequest req = new UpdateProfileRequest(nama, nip, pangkat, jabatan);
        ApiService apiService = RetrofitClient.getApiService(this);

        // Step 1: Coba POST api/profile
        executeProfileCall(apiService.updateProfile(req), req, 1, dialog, tvError);
    }

    private void executeProfileCall(Call<UpdateProfileResponse> call, UpdateProfileRequest req,
                                    int step, AlertDialog dialog, TextView tvError) {
        call.enqueue(new Callback<UpdateProfileResponse>() {
            @Override
            public void onResponse(Call<UpdateProfileResponse> c, Response<UpdateProfileResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        handleProfileUpdateSuccess(req, response.body(), dialog);
                    } else if (response.code() == 404 || response.code() == 405) {
                        // Coba fallback endpoint lainnya jika route berbeda
                        ApiService api = RetrofitClient.getApiService(ProfileActivity.this);
                        if (step == 1) {
                            executeProfileCall(api.updateProfilePut(req), req, 2, dialog, tvError);
                        } else if (step == 2) {
                            executeProfileCall(api.updateProfilePostUpdate(req), req, 3, dialog, tvError);
                        } else if (step == 3) {
                            executeProfileCall(api.updateUserPut(req), req, 4, dialog, tvError);
                        } else if (step == 4) {
                            executeProfileCall(api.updateUser(req), req, 5, dialog, tvError);
                        } else {
                            handleProfileUpdateError(response, dialog, tvError);
                        }
                    } else {
                        handleProfileUpdateError(response, dialog, tvError);
                    }
                });
            }

            @Override
            public void onFailure(Call<UpdateProfileResponse> c, Throwable t) {
                runOnUiThread(() -> {
                    showError(tvError, "Tidak dapat terhubung ke server");
                    resetPositiveButton(dialog, "Simpan");
                });
            }
        });
    }

    private void handleProfileUpdateSuccess(UpdateProfileRequest req, UpdateProfileResponse resp, AlertDialog dialog) {
        SessionManager sessionManager = new SessionManager(ProfileActivity.this);
        if (resp.getUser() != null) {
            currentUser = resp.getUser();
        } else {
            if (currentUser == null) {
                currentUser = new User();
            }
            currentUser.setName(req.getName());
            currentUser.setNip(req.getNip());
            currentUser.setPangkatGolongan(req.getPangkatGolongan());
            currentUser.setJabatan(req.getJabatan());
        }

        sessionManager.saveUserInfo(currentUser);
        bindUserProfile(currentUser);

        String msg = (resp.getMessage() != null && !resp.getMessage().isEmpty())
                ? resp.getMessage()
                : "Data profil berhasil diperbarui";
        Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
        dialog.dismiss();

        // Sync ulang dari server
        if (perkaraViewModel != null) {
            perkaraViewModel.loadUserProfile();
        }
    }

    private void handleProfileUpdateError(Response<UpdateProfileResponse> response, AlertDialog dialog, TextView tvError) {
        String msg = null;
        if (response.body() != null && response.body().getMessage() != null) {
            msg = response.body().getMessage();
        } else if (response.errorBody() != null) {
            try {
                String errStr = response.errorBody().string();
                org.json.JSONObject obj = new org.json.JSONObject(errStr);
                if (obj.has("message")) {
                    msg = obj.getString("message");
                }
            } catch (Exception ignored) {
            }
        }
        if (msg == null || msg.trim().isEmpty()) {
            msg = "Gagal memperbarui profil (HTTP " + response.code() + ")";
        }
        showError(tvError, msg);
        resetPositiveButton(dialog, "Simpan");
    }

    // ─── ViewModel & Data ────────────────────────────────────────────────────────

    private void observeAndLoad() {
        perkaraViewModel = new ViewModelProvider(this).get(PerkaraViewModel.class);

        perkaraViewModel.getUserProfileResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
            } else {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (result.isSuccess() && result.getData() != null) {
                    bindUserProfile(result.getData());
                } else if (result.isError()) {
                    Toast.makeText(this, "Gagal memuat profil: " + result.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (swipeRefresh != null) {
            swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
        }
        perkaraViewModel.loadUserProfile();
    }

    // ─── Bind UI ─────────────────────────────────────────────────────────────────

    private void bindUserProfile(User user) {
        this.currentUser = user;

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

        if (tvInfoDataPegawai != null) {
            String sub = notEmpty(user.getName(), "-");
            if (user.getJabatan() != null && !user.getJabatan().isEmpty()) {
                sub += " · " + user.getJabatan();
            }
            tvInfoDataPegawai.setText(sub);
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
