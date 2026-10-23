package com.kejaksaan.reminder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.viewmodel.PerkaraViewModel;

import java.util.List;

public class PerkaraListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvPerkara;
    private ProgressBar progressBar;
    private ProgressBar progressBarBottom;

    private LinearLayout layoutEmptyState;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyState;
    private TextView tvEmptySubMessage;
    private MaterialButton btnResetSearch;

    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private PerkaraAdapter adapter;
    private PerkaraViewModel perkaraViewModel;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perkara_list);

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        setupViewModel();

        // Tampilkan animasi mengambang SwipeRefreshLayout saat memuat awal
        if (swipeRefresh != null) {
            swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
        }

        // Initial load
        perkaraViewModel.loadFirstPage("");
    }

    private void initViews() {
        btnBack           = findViewById(R.id.btnBack);
        etSearch          = findViewById(R.id.etSearch);
        btnClearSearch    = findViewById(R.id.btnClearSearch);
        swipeRefresh      = findViewById(R.id.swipeRefresh);
        rvPerkara         = findViewById(R.id.rvPerkara);
        progressBar       = findViewById(R.id.progressBar);
        progressBarBottom = findViewById(R.id.progressBarBottom);

        layoutEmptyState  = findViewById(R.id.layoutEmptyState);
        ivEmptyIcon       = findViewById(R.id.ivEmptyIcon);
        tvEmptyState      = findViewById(R.id.tvEmptyState);
        tvEmptySubMessage = findViewById(R.id.tvEmptySubMessage);
        btnResetSearch    = findViewById(R.id.btnResetSearch);

        layoutErrorState  = findViewById(R.id.layoutErrorState);
        tvErrorMessage    = findViewById(R.id.tvErrorMessage);
        btnRetry          = findViewById(R.id.btnRetry);

        btnBack.setOnClickListener(v -> finish());

        ImageView btnRefresh = findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                perkaraViewModel.loadFirstPage(query);
            });
        }

        btnRetry.setOnClickListener(v -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            perkaraViewModel.loadFirstPage(query);
        });
        btnResetSearch.setOnClickListener(v -> {
            etSearch.setText("");
            hideKeyboard();
            if (searchRunnable != null) {
                searchHandler.removeCallbacks(searchRunnable);
            }
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            perkaraViewModel.loadFirstPage("");
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(rvPerkara, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
            return insets;
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvPerkara.setLayoutManager(layoutManager);

        adapter = new PerkaraAdapter();
        adapter.setOnItemClickListener(perkara -> {
            if (perkara != null && perkara.getId() > 0) {
                Intent intent = new Intent(PerkaraListActivity.this, DetailPerkaraActivity.class);
                intent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, perkara.getId());
                intent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, perkara.getNomorPerkara());
                startActivity(intent);
            } else {
                Toast.makeText(PerkaraListActivity.this, "Data perkara tidak tersedia", Toast.LENGTH_SHORT).show();
            }
        });
        rvPerkara.setAdapter(adapter);

        // Infinite scroll pagination
        rvPerkara.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                            && firstVisibleItemPosition >= 0) {
                        perkaraViewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            hideKeyboard();
            if (searchRunnable != null) {
                searchHandler.removeCallbacks(searchRunnable);
            }
            perkaraViewModel.loadFirstPage("");
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.length() > 0;
                btnClearSearch.setVisibility(hasText ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = (s != null) ? s.toString().trim() : "";
                // Auto-search debounce after 400ms
                searchRunnable = () -> perkaraViewModel.loadFirstPage(query);
                searchHandler.postDelayed(searchRunnable, 400);
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                hideKeyboard();
                String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                perkaraViewModel.loadFirstPage(query);
                return true;
            }
            return false;
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                Color.parseColor("#00897B"),
                Color.parseColor("#DC2626"),
                Color.parseColor("#D97706")
        );
        swipeRefresh.setOnRefreshListener(() -> {
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            perkaraViewModel.loadFirstPage(query);
        });
    }

    private void setupViewModel() {
        perkaraViewModel = new ViewModelProvider(this).get(PerkaraViewModel.class);

        perkaraViewModel.getPerkarasResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                layoutEmptyState.setVisibility(View.GONE);
                layoutErrorState.setVisibility(View.GONE);
            } else {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                if (result.isSuccess()) {
                    layoutErrorState.setVisibility(View.GONE);
                    List<Perkara> perkarasList = result.getData();

                    if (perkarasList == null || perkarasList.isEmpty()) {
                        adapter.setPerkaras(null);
                        rvPerkara.setVisibility(View.GONE);
                        layoutEmptyState.setVisibility(View.VISIBLE);

                        String currentQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                        if (!currentQuery.isEmpty()) {
                            ivEmptyIcon.setImageResource(R.drawable.ic_search);
                            tvEmptyState.setText("Perkara Tidak Ditemukan");
                            tvEmptySubMessage.setText("Tidak ada perkara yang cocok dengan \"" + currentQuery + "\"");
                            tvEmptySubMessage.setVisibility(View.VISIBLE);
                            btnResetSearch.setVisibility(View.VISIBLE);
                        } else {
                            ivEmptyIcon.setImageResource(R.drawable.ic_perkara);
                            tvEmptyState.setText("Data perkara tidak tersedia");
                            tvEmptySubMessage.setText("Belum ada perkara yang terdaftar di sistem.");
                            tvEmptySubMessage.setVisibility(View.VISIBLE);
                            btnResetSearch.setVisibility(View.GONE);
                        }
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvPerkara.setVisibility(View.VISIBLE);
                        adapter.setPerkaras(perkarasList);
                    }
                } else if (result.isError()) {
                    String errorMsg = result.getMessage() != null ? result.getMessage() : "Terjadi kesalahan";
                    if (adapter.getItemCount() == 0) {
                        layoutErrorState.setVisibility(View.VISIBLE);
                        tvErrorMessage.setText(errorMsg);
                        rvPerkara.setVisibility(View.GONE);
                        layoutEmptyState.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        perkaraViewModel.getIsPaginating().observe(this, isPaginating -> {
            if (isPaginating != null) {
                progressBarBottom.setVisibility(isPaginating ? View.VISIBLE : View.GONE);
            }
        });

        perkaraViewModel.getPaginationError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
