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
import com.kejaksaan.reminder.model.PerkaraTimeline;

import java.util.ArrayList;
import java.util.List;

public class PerkaraTimelineAdapter extends RecyclerView.Adapter<PerkaraTimelineAdapter.TimelineVerticalViewHolder> {

    private final Context context;
    private final List<PerkaraTimeline> timelineList = new ArrayList<>();

    public PerkaraTimelineAdapter(Context context) {
        this.context = context;
    }

    public void setTimelines(List<PerkaraTimeline> list) {
        this.timelineList.clear();
        if (list != null) {
            this.timelineList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineVerticalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_perkara_timeline_vertical, parent, false);
        return new TimelineVerticalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineVerticalViewHolder holder, int position) {
        PerkaraTimeline timeline = timelineList.get(position);
        boolean isFirst = (position == 0);
        boolean isLast = (position == getItemCount() - 1);
        holder.bind(timeline, isFirst, isLast);
    }

    @Override
    public int getItemCount() {
        return timelineList.size();
    }

    static class TimelineVerticalViewHolder extends RecyclerView.ViewHolder {
        View viewTopLine;
        View viewNodeDot;
        View viewBottomLine;
        TextView tvTanggalKejadian;
        TextView tvBadgeActor;
        TextView tvTahap;
        TextView tvStatusKegiatan;
        View layoutCatatan;
        TextView tvCatatan;

        public TimelineVerticalViewHolder(@NonNull View itemView) {
            super(itemView);
            viewTopLine        = itemView.findViewById(R.id.viewTopLine);
            viewNodeDot        = itemView.findViewById(R.id.viewNodeDot);
            viewBottomLine     = itemView.findViewById(R.id.viewBottomLine);
            tvTanggalKejadian  = itemView.findViewById(R.id.tvTanggalKejadian);
            tvBadgeActor       = itemView.findViewById(R.id.tvBadgeActor);
            tvTahap            = itemView.findViewById(R.id.tvTahap);
            tvStatusKegiatan   = itemView.findViewById(R.id.tvStatusKegiatan);
            layoutCatatan      = itemView.findViewById(R.id.layoutCatatan);
            tvCatatan          = itemView.findViewById(R.id.tvCatatan);
        }

        public void bind(PerkaraTimeline timeline, boolean isFirst, boolean isLast) {
            // First item doesn't have top connector, last item doesn't have bottom connector
            viewTopLine.setVisibility(isFirst ? View.INVISIBLE : View.VISIBLE);
            viewBottomLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            tvTanggalKejadian.setText(timeline.getFormattedTanggal());

            if (timeline.getTahap() != null && !timeline.getTahap().trim().isEmpty()) {
                tvTahap.setVisibility(View.VISIBLE);
                tvTahap.setText("Tahap: " + timeline.getTahap());
            } else {
                tvTahap.setVisibility(View.GONE);
            }

            tvStatusKegiatan.setText(timeline.getStatusKegiatan() != null ? timeline.getStatusKegiatan() : "-");

            // Catatan
            if (timeline.getCatatan() != null && !timeline.getCatatan().trim().isEmpty()) {
                layoutCatatan.setVisibility(View.VISIBLE);
                tvCatatan.setText(timeline.getCatatan().trim());
            } else {
                layoutCatatan.setVisibility(View.GONE);
            }

            // Actor Badge & Dot color
            if (timeline.isSystem()) {
                viewNodeDot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F46E5")));
                tvBadgeActor.setText("Sistem (Otomatis)");
                tvBadgeActor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEF2FF")));
                tvBadgeActor.setTextColor(Color.parseColor("#4F46E5"));
            } else {
                viewNodeDot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
                tvBadgeActor.setText("Petugas: " + timeline.getActorDisplayName());
                tvBadgeActor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2FE")));
                tvBadgeActor.setTextColor(Color.parseColor("#0369A1"));
            }
        }
    }
}