package com.kejaksaan.reminder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.ChangePasswordRequest;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.model.UpdatePhoneRequest;
import com.kejaksaan.reminder.model.UpdatePhoneResponse;
import com.kejaksaan.reminder.model.UpdateProfileRequest;
import com.kejaksaan.reminder.model.UpdateProfileResponse;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.util.ImageUploadHelper;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.PerkaraViewModel;

import java.io.File;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    private ImageView   imgProfileKta;
    private FrameLayout btnChangeProfilePhoto;
    private ProgressBar pbUploadPhoto;
    private TextView    tvKtaNama;
    private TextView    tvKtaJabatan;
    private TextView    tvKtaNip;
    private TextView    tvKtaPangkat;

    // ─── Settings subtitles ──────────────────────────────────────────────────────
    private TextView tvInfoDataPegawai; // subtitle Data Pegawai (Edit Profil)
    private TextView tvInfoEmail;       // subtitle Kata Sandi
    private TextView tvInfoNomorHp;     // subtitle Nomor HP
    private TextView tvSubtitleBatteryOpt; // subtitle Optimalisasi Baterai

    private ImageView ivRefreshIcon;
    private SwipeRefreshLayout swipeRefresh;
    private PerkaraViewModel perkaraViewModel;
    private User currentUser;

    private boolean isUploadingPhoto = false;
    private Uri cameraTempUri;

    // ─── Activity Result Launchers untuk Upload Foto Profil ───────────────────────
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    handlePickedImage(uri);
                }
            });

    private final ActivityResultLauncher<Intent> pickGalleryFallbackLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    handlePickedImage(result.getData().getData());
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && cameraTempUri != null) {
                    handlePickedImage(cameraTempUri);
                }
            });

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
        imgProfileKta         = findViewById(R.id.imgProfileKta);
        btnChangeProfilePhoto = findViewById(R.id.btnChangeProfilePhoto);
        pbUploadPhoto         = findViewById(R.id.pbUploadPhoto);
        tvKtaNama             = findViewById(R.id.tvKtaNama);
        tvKtaJabatan          = findViewById(R.id.tvKtaJabatan);
        tvKtaNip              = findViewById(R.id.tvKtaNip);
        tvKtaPangkat          = findViewById(R.id.tvKtaPangkat);
        tvInfoDataPegawai     = findViewById(R.id.tvInfoDataPegawai);
        tvInfoEmail           = findViewById(R.id.tvInfoEmail);
        tvInfoNomorHp         = findViewById(R.id.tvInfoNomorHp);
        tvSubtitleBatteryOpt  = findViewById(R.id.tvSubtitleBatteryOpt);
        ivRefreshIcon         = findViewById(R.id.ivRefreshIcon);
        swipeRefresh          = findViewById(R.id.swipeRefresh);

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

        // ── Preview & Ubah Foto Profil (WhatsApp Style Fullscreen Preview) ───
        View.OnClickListener previewFotoListener = v -> showDialogPreviewFoto();
        if (btnChangeProfilePhoto != null) {
            btnChangeProfilePhoto.setOnClickListener(previewFotoListener);
        }
        if (imgProfileKta != null) {
            imgProfileKta.setOnClickListener(previewFotoListener);
        }
        View cardProfileAvatar = findViewById(R.id.cardProfileAvatar);
        if (cardProfileAvatar != null) {
            cardProfileAvatar.setOnClickListener(previewFotoListener);
        }

        View btnBadgeCamera = findViewById(R.id.btnBadgeCamera);
        if (btnBadgeCamera != null) {
            btnBadgeCamera.setOnClickListener(v -> showChangePhotoDialog());
        }

        View menuFotoProfil = findViewById(R.id.menuFotoProfil);
        if (menuFotoProfil != null) {
            menuFotoProfil.setOnClickListener(previewFotoListener);
        }

        // ── Data Pegawai / Edit Profil ────────────────────────────────────────
        View menuEditProfil = findViewById(R.id.menuEditProfil);
        if (menuEditProfil != null) {
            menuEditProfil.setOnClickListener(v -> showDialogEditProfil());
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

        View menuOptimalisasiBaterai = findViewById(R.id.menuOptimalisasiBaterai);
        if (menuOptimalisasiBaterai != null) {
            updateBatteryOptSubtitle();
            menuOptimalisasiBaterai.setOnClickListener(v -> {
                if (com.kejaksaan.reminder.util.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
                    Toast.makeText(this, "✅ Aplikasi sudah bebas penghemat baterai (Alarm optimal).", Toast.LENGTH_SHORT).show();
                } else {
                    com.kejaksaan.reminder.util.BatteryOptimizationHelper.openBatteryOptimizationSettings(this);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBatteryOptSubtitle();
    }

    private void updateBatteryOptSubtitle() {
        if (tvSubtitleBatteryOpt == null) return;
        boolean isIgnoring = com.kejaksaan.reminder.util.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this);
        if (isIgnoring) {
            tvSubtitleBatteryOpt.setText("✅ Bebas Pembatasan (Alarm Optimal)");
            tvSubtitleBatteryOpt.setTextColor(Color.parseColor("#059669"));
        } else {
            tvSubtitleBatteryOpt.setText("⚠️ Dibatasi Sistem (Ketuk untuk Mengizinkan)");
            tvSubtitleBatteryOpt.setTextColor(Color.parseColor("#D97706"));
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
                                String msg = com.kejaksaan.reminder.util.ErrorMessageHelper.parseResponseError(response, "Gagal mengubah kata sandi");
                                showError(tvError, msg);
                                resetPositiveButton(dialog, "Simpan");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            showError(tvError, com.kejaksaan.reminder.util.ErrorMessageHelper.parseThrowable(t));
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
                                String msg = com.kejaksaan.reminder.util.ErrorMessageHelper.parseResponseError(response, "Gagal memperbarui nomor HP");
                                showError(tvError, msg);
                                resetPositiveButton(dialog, "Simpan");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<UpdatePhoneResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            showError(tvError, com.kejaksaan.reminder.util.ErrorMessageHelper.parseThrowable(t));
                            resetPositiveButton(dialog, "Simpan");
                        });
                    }
                });
    }

    // ─── Dialog: Edit Data Profil (Nama, NIP, NRP, Pangkat, Jabatan) ────────────

    private void showDialogEditProfil() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profil, null);

        EditText etNama    = dialogView.findViewById(R.id.etProfileNama);
        EditText etNip     = dialogView.findViewById(R.id.etProfileNip);
        EditText etNrp     = dialogView.findViewById(R.id.etProfileNrp);
        EditText etPangkat = dialogView.findViewById(R.id.etProfilePangkat);
        EditText etJabatan = dialogView.findViewById(R.id.etProfileJabatan);
        TextView tvError   = dialogView.findViewById(R.id.tvProfileError);

        // Prefill dengan data saat ini
        SessionManager sessionManager = new SessionManager(this);
        String currName = (currentUser != null && currentUser.getName() != null)
                ? currentUser.getName() : sessionManager.fetchUserName();
        String currNip = (currentUser != null && currentUser.getNip() != null)
                ? currentUser.getNip() : sessionManager.fetchUserNip();
        String currNrp = (currentUser != null && currentUser.getNrp() != null)
                ? currentUser.getNrp() : sessionManager.fetchUserNrp();
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
        if (currNrp != null && !currNrp.isEmpty() && !currNrp.equals("-")) {
            etNrp.setText(currNrp);
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
                String nrp = (etNrp != null) ? etNrp.getText().toString().trim() : "";
                String pangkat = etPangkat.getText().toString().trim();
                String jabatan = etJabatan.getText().toString().trim();

                if (nama.isEmpty()) {
                    showError(tvError, "Nama lengkap tidak boleh kosong");
                    return;
                }
                if (nip.isEmpty()) {
                    showError(tvError, "NIP tidak boleh kosong");
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

                callUpdateProfile(nama, nip, nrp, pangkat, jabatan, dialog, tvError);
            });
        });

        dialog.show();
    }

    private void callUpdateProfile(String nama, String nip, String nrp, String pangkat, String jabatan,
                                   AlertDialog dialog, TextView tvError) {
        UpdateProfileRequest req = new UpdateProfileRequest(nama, nip, nrp, pangkat, jabatan);
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
            currentUser.setNrp(req.getNrp());
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
        String msg = com.kejaksaan.reminder.util.ErrorMessageHelper.parseResponseError(response, "Gagal memperbarui profil");
        showError(tvError, msg);
        resetPositiveButton(dialog, "Simpan");
    }

    // ─── Fitur: Preview & Upload Foto Profil (WhatsApp Style Fullscreen) ─────────

    private void showDialogPreviewFoto() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_preview_foto, null);

        View rootLayout       = dialogView.findViewById(R.id.rootPreviewLayout);
        View topBar           = dialogView.findViewById(R.id.topBarPreview);
        View bottomBar        = dialogView.findViewById(R.id.bottomBarPreview);
        TextView tvNama       = dialogView.findViewById(R.id.tvPreviewNama);
        TextView tvSub        = dialogView.findViewById(R.id.tvPreviewSub);
        ImageView ivFoto      = dialogView.findViewById(R.id.ivPreviewFoto);
        ProgressBar pbLoading = dialogView.findViewById(R.id.pbPreviewLoading);
        ImageView btnClose    = dialogView.findViewById(R.id.btnPreviewClose);
        ImageView btnEdit     = dialogView.findViewById(R.id.btnPreviewEdit);
        View btnUbahFoto      = dialogView.findViewById(R.id.btnPreviewUbahFoto);

        // Pasang padding insets untuk notch kamera dan navigation bar
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 8, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        if (bottomBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
                return insets;
            });
        }

        SessionManager sessionManager = new SessionManager(this);
        String name = (currentUser != null && currentUser.getName() != null)
                ? currentUser.getName() : sessionManager.fetchUserName();
        String nip = (currentUser != null && currentUser.getNip() != null)
                ? currentUser.getNip() : sessionManager.fetchUserNip();
        String nrp = (currentUser != null && currentUser.getNrp() != null)
                ? currentUser.getNrp() : sessionManager.fetchUserNrp();
        String jabatan = (currentUser != null && currentUser.getJabatan() != null)
                ? currentUser.getJabatan() : sessionManager.fetchUserJabatan();

        if (name != null && !name.trim().isEmpty() && !name.equals("-")) {
            tvNama.setText(name);
        } else {
            tvNama.setText("Foto Profil Pegawai");
        }

        StringBuilder sub = new StringBuilder();
        if (jabatan != null && !jabatan.trim().isEmpty() && !jabatan.equals("-")) {
            sub.append(jabatan);
        }
        if (nip != null && !nip.trim().isEmpty() && !nip.equals("-")) {
            if (sub.length() > 0) sub.append(" · ");
            sub.append("NIP: ").append(nip);
        }
        if (nrp != null && !nrp.trim().isEmpty() && !nrp.equals("-")) {
            if (sub.length() > 0) sub.append(" · ");
            sub.append("NRP: ").append(nrp);
        }
        if (sub.length() > 0) {
            tvSub.setText(sub.toString());
        } else {
            tvSub.setText("Kejaksaan Negeri Ogan Komering Ulu");
        }

        // Tampilkan gambar pada popup preview
        String localPath = sessionManager.fetchLocalAvatarPath();
        File localFile = (localPath != null && !localPath.isEmpty()) ? new File(localPath) : null;
        boolean hasLocal = localFile != null && localFile.exists() && localFile.length() > 0;

        String remoteUrl = (currentUser != null) ? currentUser.getResolvedPhotoUrl() : null;
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            remoteUrl = sessionManager.fetchFotoProfilUrl();
        }

        if (pbLoading != null) {
            pbLoading.setVisibility(View.VISIBLE);
        }

        if (remoteUrl != null && !remoteUrl.trim().isEmpty()) {
            com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> errorBuilder = hasLocal
                    ? Glide.with(this).load(localFile).fitCenter()
                    : Glide.with(this).load(R.drawable.logo_kejaksaan).fitCenter();

            Glide.with(this)
                    .load(remoteUrl)
                    .placeholder(R.drawable.logo_kejaksaan)
                    .error(errorBuilder)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .fitCenter()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(ivFoto);
        } else if (hasLocal) {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            Glide.with(this)
                    .load(localFile)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .fitCenter()
                    .into(ivFoto);
        } else {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            ivFoto.setImageResource(R.drawable.logo_kejaksaan);
        }

        Dialog dialog = new Dialog(this, R.style.Theme_FullScreenPhotoDialog);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialog.getWindow().setStatusBarColor(Color.BLACK);
                dialog.getWindow().setNavigationBarColor(Color.BLACK);
            }
        }

        // WhatsApp style: ketuk foto / background untuk sembunyikan atau tampilkan bilah atas dan bawah
        View.OnClickListener toggleBars = v -> {
            if (topBar != null && bottomBar != null) {
                int nextVisibility = (topBar.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
                topBar.setVisibility(nextVisibility);
                bottomBar.setVisibility(nextVisibility);
            }
        };
        if (rootLayout != null) rootLayout.setOnClickListener(toggleBars);
        if (ivFoto != null) ivFoto.setOnClickListener(toggleBars);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                showChangePhotoDialog();
            });
        }
        if (btnUbahFoto != null) {
            btnUbahFoto.setOnClickListener(v -> {
                dialog.dismiss();
                showChangePhotoDialog();
            });
        }

        dialog.show();
    }

    private void showChangePhotoDialog() {
        if (isUploadingPhoto) {
            Toast.makeText(this, "Sedang mengunggah foto profil...", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Pilih dari Galeri", "Ambil Foto (Kamera)"};
        new AlertDialog.Builder(this)
                .setTitle("Ubah Foto Profil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openGalleryPicker();
                    } else if (which == 1) {
                        openCamera();
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openGalleryPicker() {
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
                pickMediaLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()
                );
            } else {
                launchFallbackGallery();
            }
        } catch (Exception e) {
            launchFallbackGallery();
        }
    }

    private void launchFallbackGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickGalleryFallbackLauncher.launch(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                pickGalleryFallbackLauncher.launch(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Tidak dapat membuka pemilih file perangkat", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            cameraTempUri = ImageUploadHelper.createCameraTempUri(this);
            takePictureLauncher.launch(cameraTempUri);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyiapkan kamera. Pastikan izin kamera telah diberikan pada perangkat Anda.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePickedImage(Uri uri) {
        if (uri == null) return;

        setPhotoUploadingState(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File compressedFile = ImageUploadHelper.compressImageForUpload(ProfileActivity.this, uri);
                runOnUiThread(() -> startUploadProfilePhoto(compressedFile));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setPhotoUploadingState(false);
                    Toast.makeText(ProfileActivity.this, "Gagal memproses gambar. Format berkas tidak didukung atau rusak.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setPhotoUploadingState(boolean uploading) {
        isUploadingPhoto = uploading;
        if (pbUploadPhoto != null) {
            pbUploadPhoto.setVisibility(uploading ? View.VISIBLE : View.GONE);
        }
        if (btnChangeProfilePhoto != null) {
            btnChangeProfilePhoto.setEnabled(!uploading);
            btnChangeProfilePhoto.setAlpha(uploading ? 0.6f : 1.0f);
        }
    }

    private void startUploadProfilePhoto(File file) {
        RequestBody reqFile = RequestBody.create(
                file,
                MediaType.parse("image/jpeg")
        );

        ApiService api = RetrofitClient.getApiService(this);

        // Endpoint Utama Rekomendasi: POST /api/user/foto dengan part "foto"
        MultipartBody.Part partFoto = MultipartBody.Part.createFormData("foto", file.getName(), reqFile);
        executePhotoUploadCall(api.uploadUserFoto(partFoto), file, reqFile, 1);
    }

    private void executePhotoUploadCall(Call<UpdateProfileResponse> call, File file,
                                       RequestBody reqFile, int step) {
        call.enqueue(new Callback<UpdateProfileResponse>() {
            @Override
            public void onResponse(Call<UpdateProfileResponse> c, Response<UpdateProfileResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        handlePhotoUploadSuccess(response.body(), file);
                    } else if (response.code() == 404 || response.code() == 405 || response.code() == 422) {
                        // Cascading fallback ke endpoint & nama field yang didukung backend
                        ApiService api = RetrofitClient.getApiService(ProfileActivity.this);
                        if (step == 1) {
                            // Step 2: POST /api/user/photo dengan part "photo"
                            MultipartBody.Part part = MultipartBody.Part.createFormData("photo", file.getName(), reqFile);
                            executePhotoUploadCall(api.uploadUserPhoto(part), file, reqFile, 2);
                        } else if (step == 2) {
                            // Step 3: POST /api/foto-profil dengan part "foto_profil"
                            MultipartBody.Part part = MultipartBody.Part.createFormData("foto_profil", file.getName(), reqFile);
                            executePhotoUploadCall(api.uploadFotoProfil(part), file, reqFile, 3);
                        } else if (step == 3) {
                            // Step 4: POST /api/profile/foto dengan part "foto"
                            MultipartBody.Part part = MultipartBody.Part.createFormData("foto", file.getName(), reqFile);
                            executePhotoUploadCall(api.uploadProfileFoto(part), file, reqFile, 4);
                        } else if (step == 4) {
                            // Step 5: POST /api/profile/photo dengan part "photo"
                            MultipartBody.Part part = MultipartBody.Part.createFormData("photo", file.getName(), reqFile);
                            executePhotoUploadCall(api.uploadProfilePhoto(part), file, reqFile, 5);
                        } else if (step == 5) {
                            // Step 6: POST /api/user/avatar dengan part "avatar"
                            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", file.getName(), reqFile);
                            executePhotoUploadCall(api.uploadUserAvatar(part), file, reqFile, 6);
                        } else {
                            handlePhotoUploadError(response, file);
                        }
                    } else {
                        handlePhotoUploadError(response, file);
                    }
                });
            }

            @Override
            public void onFailure(Call<UpdateProfileResponse> c, Throwable t) {
                runOnUiThread(() -> {
                    setPhotoUploadingState(false);
                    cleanupTempFile(file);
                    Toast.makeText(ProfileActivity.this, com.kejaksaan.reminder.util.ErrorMessageHelper.parseThrowable(t), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handlePhotoUploadSuccess(UpdateProfileResponse resp, File file) {
        setPhotoUploadingState(false);

        SessionManager sessionManager = new SessionManager(this);
        if (resp.getUser() != null) {
            currentUser = resp.getUser();
        } else if (currentUser != null) {
            if (resp.getData() != null && resp.getData().getFotoProfilUrl() != null) {
                currentUser.setFotoProfileUrl(resp.getData().getFotoProfilUrl());
            }
            if (resp.getData() != null && resp.getData().getFotoProfil() != null) {
                currentUser.setFotoProfil(resp.getData().getFotoProfil());
            }
        }

        // Simpan salinan lokal avatar yang baru diunggah agar selalu siap ditampilkan
        if (file != null && file.exists()) {
            try {
                int uid = currentUser != null ? currentUser.getId() : sessionManager.fetchUserId();
                File localAvatar = new File(getFilesDir(), "avatar_" + uid + ".jpg");
                ImageUploadHelper.copyFile(file, localAvatar);
                sessionManager.saveLocalAvatarPath(localAvatar.getAbsolutePath());
            } catch (Exception e) {
                android.util.Log.e("SIPETA_PROFILE", "Gagal menyimpan avatar lokal", e);
            }
            cleanupTempFile(file);
        }

        if (currentUser != null) {
            sessionManager.saveUserInfo(currentUser);
        }

        String msg = resp.getMessage();
        Toast.makeText(this,
                (msg != null && !msg.isEmpty()) ? msg : "Foto profil berhasil diperbarui",
                Toast.LENGTH_SHORT).show();

        // Muat gambar terbaru ke UI
        loadAvatarImage(currentUser);

        // Sinkronisasi data user terbaru dengan server
        if (perkaraViewModel != null) {
            perkaraViewModel.loadUserProfile();
        }
    }

    private void loadAvatarImage(User user) {
        if (imgProfileKta == null) return;

        SessionManager sm = new SessionManager(this);
        String localPath = sm.fetchLocalAvatarPath();
        File localFile = (localPath != null && !localPath.isEmpty()) ? new File(localPath) : null;
        boolean hasLocal = localFile != null && localFile.exists() && localFile.length() > 0;

        String remoteUrl = (user != null) ? user.getResolvedPhotoUrl() : null;
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            remoteUrl = sm.fetchFotoProfilUrl();
        }

        android.util.Log.d("SIPETA_PROFILE", "loadAvatarImage: remoteUrl=" + remoteUrl + ", hasLocal=" + hasLocal);

        if (remoteUrl != null && !remoteUrl.trim().isEmpty()) {
            com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> errorBuilder = hasLocal
                    ? Glide.with(this).load(localFile).centerCrop()
                    : Glide.with(this).load(R.drawable.logo_kejaksaan).centerCrop();

            Glide.with(this)
                    .load(remoteUrl)
                    .placeholder(R.drawable.logo_kejaksaan)
                    .error(errorBuilder)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .centerCrop()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("SIPETA_PROFILE", "Glide gagal memuat remoteUrl: " + model, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("SIPETA_PROFILE", "Glide sukses memuat remoteUrl: " + model);
                            return false;
                        }
                    })
                    .into(imgProfileKta);
        } else if (hasLocal) {
            Glide.with(this)
                    .load(localFile)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .centerCrop()
                    .into(imgProfileKta);
        } else {
            imgProfileKta.setImageResource(R.drawable.logo_kejaksaan);
        }
    }

    private void handlePhotoUploadError(Response<UpdateProfileResponse> response, File file) {
        setPhotoUploadingState(false);
        cleanupTempFile(file);
        String msg = com.kejaksaan.reminder.util.ErrorMessageHelper.parseResponseError(response, "Gagal memperbarui foto profil");
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                file.delete();
            } catch (Exception ignored) {
            }
        }
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
                    String msg = result.getMessage() != null ? result.getMessage() : "Gagal memuat profil pengguna";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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

        loadAvatarImage(user);

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
