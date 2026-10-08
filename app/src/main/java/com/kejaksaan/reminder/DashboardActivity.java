package com.kejaksaan.reminder;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.viewmodel.PerkaraViewModel;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvUserName;
    private TextView tvUserRole;
    private ImageView imgProfileAvatar;

    private PerkaraViewModel perkaraViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Top padding for status bar (applied to profile card to avoid pushing background)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardUserProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + 16, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Bottom padding for navigation bar background (so background reaches bottom)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarBackground), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 12); // Add initial 12dp padding
            return insets;
        });

        // Initialize Views
        tvUserName = findViewById(R.id.tvUserName);
        tvUserRole = findViewById(R.id.tvUserRole);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);

        // Initialize ViewModel
        perkaraViewModel = new ViewModelProvider(this).get(PerkaraViewModel.class);

        // Observe profile changes
        perkaraViewModel.getUserProfileResult().observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                bindUserProfile(result.getData());
            } else if (result.isError()) {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch API
        perkaraViewModel.loadUserProfile();

        // Setup Logout functionality
        com.kejaksaan.reminder.viewmodel.AuthViewModel authViewModel = new ViewModelProvider(this).get(com.kejaksaan.reminder.viewmodel.AuthViewModel.class);
        
        android.view.View.OnClickListener logoutListener = v -> {
            authViewModel.logout();
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        };

        findViewById(R.id.btnLogout).setOnClickListener(logoutListener);
        findViewById(R.id.cardBtnLogout).setOnClickListener(logoutListener);

        // Menu Perkara
        findViewById(R.id.menuPerkara).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, com.kejaksaan.reminder.PerkaraListActivity.class);
            startActivity(intent);
        });
    }

    private void bindUserProfile(User user) {
        tvUserName.setText(user.getName());
        
        // Show jabatan if available, otherwise fallback to role
        String jabatanOrRole = user.getJabatan() != null && !user.getJabatan().isEmpty() 
            ? user.getJabatan() 
            : user.getRole();
        tvUserRole.setText(jabatanOrRole);

        // Load image using Glide
        if (user.getFotoProfil() != null && !user.getFotoProfil().isEmpty()) {
            /* 
             * If fotoProfil is just a path like "folder/image.jpg", 
             * make sure to prepend the base URL here (e.g., RetrofitClient.BASE_URL + "storage/" + user.getFotoProfil()) 
             * If it's already a full URL from the backend, just load it directly. 
             */
            String photoUrl = user.getFotoProfil().startsWith("http") 
                ? user.getFotoProfil() 
                : com.kejaksaan.reminder.api.RetrofitClient.BASE_URL + "storage/" + user.getFotoProfil();

            Glide.with(this)
                 .load(photoUrl)
                 .placeholder(R.drawable.logo_kejaksaan)
                 .error(R.drawable.logo_kejaksaan)
                 .circleCrop()
                 .into(imgProfileAvatar);
        }
    }
}
