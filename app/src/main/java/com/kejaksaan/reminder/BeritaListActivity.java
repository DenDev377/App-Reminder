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
import com.kejaksaan.reminder.adapter.BeritaAdapter;
import com.kejaksaan.reminder.model.Berita;
import com.kejaksaan.reminder.viewmodel.BeritaViewModel;

import java.util.List;

public class BeritaListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnRefresh;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvBerita;
    private ProgressBar progressBarBottom;

    private LinearLayout layoutEmptyState;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyState;
    private TextView tvEmptySubMessage;
    private MaterialButton btnResetSearch;

    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private BeritaAdapter adapter;
    private BeritaViewModel viewModel;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_berita_list);

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        setupViewModel();

        if (swipeRefresh != null) {
            swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
        }

        viewModel.loadFirstPage("");
    }

    private void initViews() {
        btnBack           = findViewById(R.id.btnBack);
        btnRefresh        = findViewById(R.id.btnRefresh);
        etSearch          = findViewById(R.id.etSearch);
        btnClearSearch    = findViewById(R.id.btnClearSearch);
        swipeRefresh      = findViewById(R.id.swipeRefresh);
        rvBerita          = findViewById(R.id.rvBerita);
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
        btnRefresh.setOnClickListener(v -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            viewModel.refresh();
        });

        btnRetry.setOnClickListener(v -> {
            layoutErrorState.setVisibility(View.GONE);
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            viewModel.refresh();
        });

        btnResetSearch.setOnClickListener(v -> {
            etSearch.setText("");
            hideKeyboard();
            viewModel.loadFirstPage("");
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvBerita.setLayoutManager(layoutManager);
        adapter = new BeritaAdapter(this);
        rvBerita.setAdapter(adapter);

        adapter.setOnBeritaClickListener(berita -> {
            Intent intent = new Intent(BeritaListActivity.this, DetailBeritaActivity.class);
            intent.putExtra(DetailBeritaActivity.EXTRA_BERITA, berita);
            startActivity(intent);
        });

        rvBerita.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!viewModel.isLoading() && !viewModel.isLastPage()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                            && firstVisibleItemPosition >= 0) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(true);
                    }
                    viewModel.loadFirstPage(query);
                };

                searchHandler.postDelayed(searchRunnable, 400);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            hideKeyboard();
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                viewModel.loadFirstPage(etSearch.getText().toString().trim());
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
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BeritaViewModel.class);

        viewModel.getBeritasResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                if (!swipeRefresh.isRefreshing()) {
                    swipeRefresh.setRefreshing(true);
                }
            } else if (result.isSuccess()) {
                swipeRefresh.setRefreshing(false);
                layoutErrorState.setVisibility(View.GONE);

                List<Berita> beritas = result.getData();
                if (beritas == null || beritas.isEmpty()) {
                    adapter.setBeritaList(null);
                    showEmptyState();
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvBerita.setVisibility(View.VISIBLE);
                    adapter.setBeritaList(beritas);
                }
            } else if (result.isError()) {
                swipeRefresh.setRefreshing(false);
                if (adapter.getItemCount() == 0) {
                    showErrorState(result.getMessage());
                } else {
                    Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getIsPaginating().observe(this, isPaginating -> {
            if (isPaginating != null) {
                progressBarBottom.setVisibility(isPaginating ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getPaginationError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        rvBerita.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);

        String query = viewModel.getSearchKeyword();
        if (query != null && !query.isEmpty()) {
            tvEmptyState.setText("Berita Tidak Ditemukan");
            tvEmptySubMessage.setText("Tidak ada berita yang cocok dengan kata kunci \"" + query + "\".");
            btnResetSearch.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setText("Belum Ada Berita");
            tvEmptySubMessage.setText("Daftar berita publikasi saat ini belum tersedia.");
            btnResetSearch.setVisibility(View.GONE);
        }
    }

    private void showErrorState(String message) {
        rvBerita.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message != null ? message : "Terjadi kesalahan saat memuat berita.");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}

