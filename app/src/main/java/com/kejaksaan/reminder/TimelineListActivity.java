package com.kejaksaan.reminder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.kejaksaan.reminder.adapter.TimelineAdapter;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.TimelineViewModel;

import java.util.List;

public class TimelineListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvTimelines;
    private ProgressBar progressBarMain;
    private ProgressBar progressBarBottom;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private TimelineAdapter adapter;
    private TimelineViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_timeline_list);

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

        viewModel.loadFirstPage("");
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);
        etSearch           = findViewById(R.id.etSearch);
        btnClearSearch     = findViewById(R.id.btnClearSearch);
        swipeRefresh       = findViewById(R.id.swipeRefresh);
        rvTimelines        = findViewById(R.id.rvTimelines);
        progressBarMain    = findViewById(R.id.progressBarMain);
        progressBarBottom  = findViewById(R.id.progressBarBottom);
        layoutEmptyState   = findViewById(R.id.layoutEmptyState);
        tvEmptyMessage     = findViewById(R.id.tvEmptyMessage);
        layoutErrorState   = findViewById(R.id.layoutErrorState);
        tvErrorMessage     = findViewById(R.id.tvErrorMessage);
        btnRetry           = findViewById(R.id.btnRetry);

        btnBack.setOnClickListener(v -> finish());

        ImageView btnRefresh = findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                String query = etSearch.getText().toString().trim();
                viewModel.loadFirstPage(query);
            });
        }

        btnRetry.setOnClickListener(v -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            viewModel.loadFirstPage(etSearch.getText().toString().trim());
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(rvTimelines, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
            return insets;
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvTimelines.setLayoutManager(layoutManager);

        adapter = new TimelineAdapter(this);
        adapter.setOnItemClickListener(perkara -> {
            if (perkara != null && perkara.getId() > 0) {
                Intent intent = new Intent(TimelineListActivity.this, DetailPerkaraActivity.class);
                intent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, perkara.getId());
                if (perkara.getNomorPerkara() != null && !perkara.getNomorPerkara().isEmpty()) {
                    intent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, perkara.getNomorPerkara());
                }
                startActivity(intent);
            } else {
                Toast.makeText(TimelineListActivity.this, "Data perkara tidak tersedia", Toast.LENGTH_SHORT).show();
            }
        });
        rvTimelines.setAdapter(adapter);

        rvTimelines.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                            && firstVisibleItemPosition >= 0) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            hideKeyboard();
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            viewModel.loadFirstPage("");
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    btnClearSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                String query = etSearch.getText().toString().trim();
                viewModel.loadFirstPage(query);
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
            String query = etSearch.getText().toString().trim();
            viewModel.loadFirstPage(query);
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TimelineViewModel.class);

        viewModel.getPerkarasResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                if (progressBarMain != null) {
                    progressBarMain.setVisibility(View.GONE);
                }
                layoutEmptyState.setVisibility(View.GONE);
                layoutErrorState.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (progressBarMain != null) {
                    progressBarMain.setVisibility(View.GONE);
                }
                layoutErrorState.setVisibility(View.GONE);

                List<Perkara> list = result.getData();
                if (list == null || list.isEmpty()) {
                    adapter.setPerkaras(null);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvTimelines.setVisibility(View.GONE);

                    String currentSearch = etSearch.getText().toString().trim();
                    if (!currentSearch.isEmpty()) {
                        tvEmptyMessage.setText("Tidak ada perkara yang cocok dengan \"" + currentSearch + "\"");
                    } else {
                        tvEmptyMessage.setText("Daftar timeline perkara belum tersedia");
                    }
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvTimelines.setVisibility(View.VISIBLE);
                    adapter.setPerkaras(list);
                }
            } else if (result.isError()) {
                swipeRefresh.setRefreshing(false);
                progressBarMain.setVisibility(View.GONE);

                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat data perkara";

                if (errorMsg.contains("UNAUTHORIZED:401")) {
                    SessionManager sessionManager = new SessionManager(this);
                    sessionManager.clearSession();
                    Toast.makeText(this, "Sesi telah berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }

                if (adapter.getItemCount() == 0) {
                    layoutErrorState.setVisibility(View.VISIBLE);
                    tvErrorMessage.setText(errorMsg);
                    rvTimelines.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
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

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}