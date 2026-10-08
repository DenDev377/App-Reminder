package com.kejaksaan.reminder;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.viewmodel.PerkaraViewModel;

import java.util.List;

public class PerkaraListActivity extends AppCompatActivity {

    private RecyclerView rvPerkara;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private PerkaraAdapter adapter;
    private PerkaraViewModel perkaraViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perkara_list);

        // System bars padding (Top for Header, Bottom for RecyclerView)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rvPerkara), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
            return insets;
        });

        // Init views
        rvPerkara = findViewById(R.id.rvPerkara);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvPerkara.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PerkaraAdapter();
        rvPerkara.setAdapter(adapter);

        // Setup ViewModel
        perkaraViewModel = new ViewModelProvider(this).get(PerkaraViewModel.class);

        perkaraViewModel.getPerkarasResult().observe(this, result -> {
            if (result.isLoading()) {
                progressBar.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
                rvPerkara.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                progressBar.setVisibility(View.GONE);
                List<Perkara> perkarasList = result.getData();
                
                if (perkarasList == null || perkarasList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvPerkara.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvPerkara.setVisibility(View.VISIBLE);
                    adapter.setPerkaras(perkarasList);
                }
            } else if (result.isError()) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText(result.getMessage());
                rvPerkara.setVisibility(View.GONE);
            }
        });

        // Fetch data
        perkaraViewModel.loadPerkaras();
    }
}
