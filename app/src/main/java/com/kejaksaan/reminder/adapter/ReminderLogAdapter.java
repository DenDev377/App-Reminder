package com.kejaksaan.reminder.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.model.PerkaraReminderLogGroup;
import com.kejaksaan.reminder.model.ReminderLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter RecyclerView untuk menampilkan daftar Perkara (1 Card per Nomor Perkara/SPDP)
 * pada menu Log Reminders SLA. Seluruh riwayat tahapan SLA dari perkara yang sama dikelompokkan
 * di dalam satu card perkara.
 */
public class ReminderLogAdapter extends RecyclerView.Adapter<ReminderLogAdapter.LogViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PerkaraReminderLogGroup group);
    }

    private final Context context;
    private final List<PerkaraReminderLogGroup> groupList = new ArrayList<>();
    private final Set<Integer> perkaraIds = new HashSet<>();
    private OnItemClickListener listener;

    public ReminderLogAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Mengganti seluruh data grup perkara (saat filter berubah atau refresh).
     */
    public void setGroups(List<PerkaraReminderLogGroup> list) {
        this.groupList.clear();
        this.perkaraIds.clear();
        if (list != null) {
            for (PerkaraReminderLogGroup g : list) {
                if (g != null && !perkaraIds.contains(g.getPerkaraId())) {
                    perkaraIds.add(g.getPerkaraId());
                    this.groupList.add(g);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Menambahkan item baru atau memperbarui grup perkara yang sudah ada (untuk pagination).
     */
    public void appendGroups(List<PerkaraReminderLogGroup> newGroups) {
        if (newGroups == null || newGroups.isEmpty()) return;

        int startPosition = this.groupList.size();
        int addedCount = 0;

        for (PerkaraReminderLogGroup g : newGroups) {
            if (g != null && !perkaraIds.contains(g.getPerkaraId())) {
                perkaraIds.add(g.getPerkaraId());
                this.groupList.add(g);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            notifyItemRangeInserted(startPosition, addedCount);
        } else {
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        PerkaraReminderLogGroup group = groupList.get(position);
        holder.bind(group, listener, context);
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardReminderLog;
        TextView tvNomorPerkara;
        TextView tvBadgeTotalTahap;
        TextView tvPrimaryStatus;
        TextView tvTahapSaatIni;
        TextView tvStatusSaatIni;
        TextView tvTersangka;
        TextView tvJaksa;
        TextView tvPenyidik;
        LinearLayout layoutSlaStagesList;
        TextView tvToggleExpand;
        TextView btnViewPerkara;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            cardReminderLog     = itemView.findViewById(R.id.cardReminderLog);
            tvNomorPerkara      = itemView.findViewById(R.id.tvNomorPerkara);
            tvBadgeTotalTahap   = itemView.findViewById(R.id.tvBadgeTotalTahap);
            tvPrimaryStatus     = itemView.findViewById(R.id.tvPrimaryStatus);
            tvTahapSaatIni      = itemView.findViewById(R.id.tvTahapSaatIni);
            tvStatusSaatIni     = itemView.findViewById(R.id.tvStatusSaatIni);
            tvTersangka         = itemView.findViewById(R.id.tvTersangka);
            tvJaksa             = itemView.findViewById(R.id.tvJaksa);
            tvPenyidik          = itemView.findViewById(R.id.tvPenyidik);
            layoutSlaStagesList = itemView.findViewById(R.id.layoutSlaStagesList);
            tvToggleExpand      = itemView.findViewById(R.id.tvToggleExpand);
            btnViewPerkara      = itemView.findViewById(R.id.btnViewPerkara);
        }

        public void bind(PerkaraReminderLogGroup group, OnItemClickListener listener, Context context) {
            tvNomorPerkara.setText(group.getNomorPerkara());

            int totalStages = group.getTotalStages();
            tvBadgeTotalTahap.setText(totalStages + " Tahap SLA");

            String primaryStatus = group.getPrimaryStatusBadge();
            tvPrimaryStatus.setText(primaryStatus);
            applyPrimaryStatusStyle(primaryStatus);

            tvTahapSaatIni.setText(group.getTahapSaatIni());
            tvStatusSaatIni.setText("Status: " + group.getStatusSaatIni());
            tvTersangka.setText("Tersangka: " + group.getTersangkaNames());
            tvJaksa.setText("Jaksa: " + group.getJaksaName());
            tvPenyidik.setText("Penyidik: " + group.getPenyidik());

            // Render sub-stages list
            renderSubStages(group, context);

            // Click listener
            View.OnClickListener clickAction = v -> {
                if (listener != null) {
                    listener.onItemClick(group);
                }
            };
            cardReminderLog.setOnClickListener(clickAction);
            btnViewPerkara.setOnClickListener(clickAction);
        }

        private void renderSubStages(PerkaraReminderLogGroup group, Context context) {
            layoutSlaStagesList.removeAllViews();
            List<ReminderLog> logs = group.getReminderLogs();
            if (logs == null || logs.isEmpty()) {
                tvToggleExpand.setVisibility(View.GONE);
                return;
            }

            int totalCount = logs.size();
            boolean isExpanded = group.isExpanded();
            int displayCount = (totalCount <= 2 || isExpanded) ? totalCount : 2;

            LayoutInflater inflater = LayoutInflater.from(context);

            for (int i = 0; i < displayCount; i++) {
                ReminderLog log = logs.get(i);
                View stageView = inflater.inflate(R.layout.item_sub_reminder_stage, layoutSlaStagesList, false);

                View viewStageDot        = stageView.findViewById(R.id.viewStageDot);
                TextView tvSubStageTahap = stageView.findViewById(R.id.tvSubStageTahap);
                TextView tvSubStageJenis = stageView.findViewById(R.id.tvSubStageJenis);
                TextView tvSubStageStatusLog = stageView.findViewById(R.id.tvSubStageStatusLog);
                TextView tvSubStagePeriode = stageView.findViewById(R.id.tvSubStagePeriode);
                TextView tvSubStageSisaHari = stageView.findViewById(R.id.tvSubStageSisaHari);
                View viewSubStageDivider = stageView.findViewById(R.id.viewSubStageDivider);

                String tahapTitle = log.getTahap() != null && !log.getTahap().isEmpty() ? log.getTahap() : "Tahapan SLA";
                tvSubStageTahap.setText(tahapTitle);

                String jenis = log.getJenisReminder() != null ? log.getJenisReminder() : "Reminder";
                tvSubStageJenis.setText(jenis);

                String statusLog = log.getStatusLog() != null ? log.getStatusLog() : (log.isActive() ? "Aktif" : "Selesai");
                tvSubStageStatusLog.setText(statusLog);

                int badgeColor;
                int dotColor;
                String lower = statusLog.toLowerCase();
                if (lower.contains("overdue")) {
                    badgeColor = Color.parseColor("#EF4444");
                    dotColor   = Color.parseColor("#EF4444");
                } else if (lower.contains("selesai")) {
                    badgeColor = Color.parseColor("#10B981");
                    dotColor   = Color.parseColor("#10B981");
                } else if (lower.contains("aktif")) {
                    badgeColor = Color.parseColor("#0284C7");
                    dotColor   = Color.parseColor("#0284C7");
                } else {
                    badgeColor = Color.parseColor("#64748B");
                    dotColor   = Color.parseColor("#64748B");
                }
                tvSubStageStatusLog.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
                tvSubStageStatusLog.setTextColor(Color.WHITE);
                viewStageDot.setBackgroundTintList(ColorStateList.valueOf(dotColor));

                String tglMulai = ReminderLog.formatDate(log.getTanggalMulai());
                String tglDeadline = ReminderLog.formatDate(log.getDeadline());
                tvSubStagePeriode.setText(tglMulai + "  ➔  " + tglDeadline);

                int sisa = log.getSisaHari();
                if (sisa < 0) {
                    tvSubStageSisaHari.setText("Lewat " + Math.abs(sisa) + " Hari");
                    tvSubStageSisaHari.setTextColor(Color.parseColor("#DC2626"));
                } else if (sisa == 0) {
                    tvSubStageSisaHari.setText("Jatuh Tempo Hari Ini");
                    tvSubStageSisaHari.setTextColor(Color.parseColor("#D97706"));
                } else {
                    tvSubStageSisaHari.setText("Sisa " + sisa + " Hari");
                    tvSubStageSisaHari.setTextColor(Color.parseColor("#059669"));
                }

                // Sembunyikan divider pada item terakhir yang ditampilkan
                if (i == displayCount - 1) {
                    viewSubStageDivider.setVisibility(View.GONE);
                } else {
                    viewSubStageDivider.setVisibility(View.VISIBLE);
                }

                layoutSlaStagesList.addView(stageView);
            }

            // Atur tombol toggle expand jika tahapan lebih dari 2
            if (totalCount > 2) {
                tvToggleExpand.setVisibility(View.VISIBLE);
                if (isExpanded) {
                    tvToggleExpand.setText("Sembunyikan Tahapan ▲");
                } else {
                    tvToggleExpand.setText("Lihat Semua (" + totalCount + " Tahapan) ▼");
                }
                tvToggleExpand.setOnClickListener(v -> {
                    group.setExpanded(!group.isExpanded());
                    renderSubStages(group, context);
                });
            } else {
                tvToggleExpand.setVisibility(View.GONE);
            }
        }

        private void applyPrimaryStatusStyle(String status) {
            int badgeColor;
            int strokeColor;

            if (status.equalsIgnoreCase("Overdue")) {
                badgeColor  = Color.parseColor("#EF4444");
                strokeColor = Color.parseColor("#FECACA");
            } else if (status.equalsIgnoreCase("Aktif")) {
                badgeColor  = Color.parseColor("#0284C7");
                strokeColor = Color.parseColor("#BAE6FD");
            } else if (status.equalsIgnoreCase("Selesai")) {
                badgeColor  = Color.parseColor("#10B981");
                strokeColor = Color.parseColor("#A7F3D0");
            } else {
                badgeColor  = Color.parseColor("#64748B");
                strokeColor = Color.parseColor("#E2E8F0");
            }

            tvPrimaryStatus.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
            tvPrimaryStatus.setTextColor(Color.WHITE);

            if (cardReminderLog != null) {
                cardReminderLog.setStrokeColor(strokeColor);
            }
        }
    }
}
