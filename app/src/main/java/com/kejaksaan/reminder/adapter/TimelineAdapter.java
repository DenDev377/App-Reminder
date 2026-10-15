package com.kejaksaan.reminder.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.Tersangka;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Perkara perkara);
    }

    private final Context context;
    private final List<Perkara> perkaraList = new ArrayList<>();
    private final Set<Integer> perkaraIds = new HashSet<>();
    private OnItemClickListener listener;

    public TimelineAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setPerkaras(List<Perkara> list) {
        this.perkaraList.clear();
        this.perkaraIds.clear();
        if (list != null) {
            for (Perkara item : list) {
                if (item != null && !perkaraIds.contains(item.getId())) {
                    perkaraIds.add(item.getId());
                    this.perkaraList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void appendPerkaras(List<Perkara> newItems) {
        if (newItems == null || newItems.isEmpty()) return;

        int startPosition = this.perkaraList.size();
        int addedCount = 0;

        for (Perkara item : newItems) {
            if (item != null && !perkaraIds.contains(item.getId())) {
                perkaraIds.add(item.getId());
                this.perkaraList.add(item);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            notifyItemRangeInserted(startPosition, addedCount);
        }
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline_card, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        Perkara perkara = perkaraList.get(position);
        holder.bind(perkara, listener);
    }

    @Override
    public int getItemCount() {
        return perkaraList.size();
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView tvNomorPerkara;
        TextView tvTahap;
        TextView tvStatusKegiatan;
        View layoutCatatan;
        TextView tvCatatan;
        TextView tvTanggalKejadian;
        TextView tvBadgeActor;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomorPerkara     = itemView.findViewById(R.id.tvNomorPerkara);
            tvTahap            = itemView.findViewById(R.id.tvTahap);
            tvStatusKegiatan   = itemView.findViewById(R.id.tvStatusKegiatan);
            layoutCatatan      = itemView.findViewById(R.id.layoutCatatan);
            tvCatatan          = itemView.findViewById(R.id.tvCatatan);
            tvTanggalKejadian  = itemView.findViewById(R.id.tvTanggalKejadian);
            tvBadgeActor       = itemView.findViewById(R.id.tvBadgeActor);
        }

        public void bind(Perkara perkara, OnItemClickListener listener) {
            if (perkara.getNomorPerkara() != null && !perkara.getNomorPerkara().isEmpty()) {
                tvNomorPerkara.setText(perkara.getNomorPerkara());
            } else {
                tvNomorPerkara.setText("Perkara #" + perkara.getId());
            }

            // Tahap Saat Ini
            if (perkara.getTahapSaatIni() != null && !perkara.getTahapSaatIni().isEmpty()) {
                tvTahap.setVisibility(View.VISIBLE);
                tvTahap.setText(perkara.getTahapSaatIni());
            } else {
                tvTahap.setVisibility(View.GONE);
            }

            // Status Kegiatan Saat Ini
            if (perkara.getStatusSaatIni() != null && !perkara.getStatusSaatIni().isEmpty()) {
                tvStatusKegiatan.setText(perkara.getStatusSaatIni());
            } else {
                tvStatusKegiatan.setText("Status berkas belum diperbarui");
            }

            // Tersangka
            List<Tersangka> tersangkaList = perkara.getTersangkas();
            if (tersangkaList != null && !tersangkaList.isEmpty()) {
                layoutCatatan.setVisibility(View.VISIBLE);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < tersangkaList.size(); i++) {
                    sb.append(i + 1).append(". ").append(tersangkaList.get(i).getNama());
                    if (i < tersangkaList.size() - 1) {
                        sb.append(", ");
                    }
                }
                tvCatatan.setText(sb.toString());
            } else {
                layoutCatatan.setVisibility(View.GONE);
            }

            // Penyidik
            if (perkara.getPenyidik() != null && !perkara.getPenyidik().trim().isEmpty()) {
                tvTanggalKejadian.setText("Penyidik: " + perkara.getPenyidik().trim());
            } else {
                tvTanggalKejadian.setText("Penyidik: -");
            }

            // Button Badge "Buka Timeline"
            tvBadgeActor.setText("Lihat Timeline \u2192");
            tvBadgeActor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2FE")));
            tvBadgeActor.setTextColor(Color.parseColor("#0369A1"));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(perkara);
                }
            });
        }
    }
}