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

public class BeritaAdapter extends RecyclerView.Adapter<BeritaAdapter.BeritaViewHolder> {

    public interface OnBeritaClickListener {
        void onBeritaClick(Berita berita);
    }

    private final Context context;
    private final List<Berita> beritaList = new ArrayList<>();
    private OnBeritaClickListener listener;

    public BeritaAdapter(Context context) {
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
    public BeritaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_berita, parent, false);
        return new BeritaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BeritaViewHolder holder, int position) {
        Berita berita = beritaList.get(position);
        holder.bind(berita, listener);
    }

    @Override
    public int getItemCount() {
        return beritaList.size();
    }

    public class BeritaViewHolder extends RecyclerView.ViewHolder {

        private final View cardBerita;
        private final ImageView ivThumbnail;
        private final TextView tvJudul;
        private final TextView tvRingkasan;
        private final TextView tvTanggal;
        private final TextView tvPenulisTag;

        public BeritaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBerita = itemView.findViewById(R.id.cardBerita);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvJudul = itemView.findViewById(R.id.tvJudul);
            tvRingkasan = itemView.findViewById(R.id.tvRingkasan);
            tvTanggal = itemView.findViewById(R.id.tvTanggal);
            tvPenulisTag = itemView.findViewById(R.id.tvPenulisTag);
        }

        public void bind(Berita berita, OnBeritaClickListener clickListener) {
            if (berita == null) return;

            tvJudul.setText(berita.getJudul() != null ? berita.getJudul() : "-");

            if (berita.getRingkasan() != null && !berita.getRingkasan().trim().isEmpty()) {
                tvRingkasan.setVisibility(View.VISIBLE);
                tvRingkasan.setText(berita.getRingkasan());
            } else {
                tvRingkasan.setVisibility(View.GONE);
            }

            tvTanggal.setText(berita.getFormattedTanggal());
            tvPenulisTag.setText(berita.getPenulis());

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
                // Tampilkan lambang kejaksaan dengan padding rapi
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
