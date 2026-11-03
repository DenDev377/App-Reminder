package com.kejaksaan.reminder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.model.Berita;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter untuk preview berita horizontal di Dashboard.
 */
public class BeritaDashboardAdapter extends RecyclerView.Adapter<BeritaDashboardAdapter.ViewHolder> {

    public interface OnBeritaClickListener {
        void onBeritaClick(Berita berita);
    }

    private final Context context;
    private final List<Berita> beritaList = new ArrayList<>();
    private OnBeritaClickListener listener;

    public BeritaDashboardAdapter(Context context) {
        this.context = context;
    }

    public void setOnBeritaClickListener(OnBeritaClickListener listener) {
        this.listener = listener;
    }

    public void setBeritaList(List<Berita> list) {
        this.beritaList.clear();
        if (list != null) {
            this.beritaList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_berita_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Berita berita = beritaList.get(position);
        holder.bind(berita, listener);
    }

    @Override
    public int getItemCount() {
        return beritaList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final View cardBerita;
        private final ImageView ivThumbnail;
        private final TextView tvJudul;
        private final TextView tvTanggal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBerita = itemView.findViewById(R.id.cardBerita);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvJudul = itemView.findViewById(R.id.tvJudul);
            tvTanggal = itemView.findViewById(R.id.tvTanggal);
        }

        public void bind(Berita berita, OnBeritaClickListener clickListener) {
            if (berita == null) return;

            tvJudul.setText(berita.getJudul() != null ? berita.getJudul() : "-");
            tvTanggal.setText(berita.getFormattedTanggal());

            String imageUrl = berita.getResolvedImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(context)
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.lambang_kejaksaan)
                        .error(R.drawable.lambang_kejaksaan)
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                ivThumbnail.setImageResource(R.drawable.lambang_kejaksaan);
            }

            cardBerita.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onBeritaClick(berita);
                }
            });
        }
    }
}
