package com.kejaksaan.reminder;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.Tersangka;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerkaraAdapter extends RecyclerView.Adapter<PerkaraAdapter.PerkaraViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Perkara perkara);
    }

    private final List<Perkara> perkaraList = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setPerkaras(List<Perkara> list) {
        this.perkaraList.clear();
        if (list != null) {
            this.perkaraList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PerkaraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_perkara, parent, false);
        return new PerkaraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PerkaraViewHolder holder, int position) {
        Perkara perkara = perkaraList.get(position);
        holder.bind(perkara, listener);
    }

    @Override
    public int getItemCount() {
        return perkaraList.size();
    }

    static class PerkaraViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNomorPerkara;
        private final TextView tvStatusSaatIni;
        private final View layoutStatusDetail;
        private final TextView tvStatusDetail;
        private final TextView tvTahapSaatIni;
        private final TextView tvTersangkas;

        public PerkaraViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomorPerkara     = itemView.findViewById(R.id.tvNomorPerkara);
            tvStatusSaatIni    = itemView.findViewById(R.id.tvStatusSaatIni);
            layoutStatusDetail = itemView.findViewById(R.id.layoutStatusDetail);
            tvStatusDetail     = itemView.findViewById(R.id.tvStatusDetail);
            tvTahapSaatIni     = itemView.findViewById(R.id.tvTahapSaatIni);
            tvTersangkas       = itemView.findViewById(R.id.tvTersangkas);
        }

        public void bind(Perkara perkara, OnItemClickListener listener) {
            tvNomorPerkara.setText(perkara.getNomorPerkara() != null ? perkara.getNomorPerkara() : "-");

            // Format status badge agar ringkas & tidak merusak layout
            String rawStatus = perkara.getStatusSaatIni();
            if (rawStatus != null && !rawStatus.trim().isEmpty()) {
                String compactStatus = getCompactStatus(rawStatus);
                tvStatusSaatIni.setVisibility(View.VISIBLE);
                tvStatusSaatIni.setText(compactStatus);
                applyBadgeStyle(tvStatusSaatIni, compactStatus);

                // Jika status aslinya memiliki keterangan panjang (misal P-20 (Pengembalian SPDP)),
                // tampilkan keterangan lengkap di body kartu
                if (layoutStatusDetail != null && tvStatusDetail != null) {
                    if (!rawStatus.trim().equalsIgnoreCase(compactStatus)) {
                        layoutStatusDetail.setVisibility(View.VISIBLE);
                        tvStatusDetail.setText(rawStatus.trim());
                    } else {
                        layoutStatusDetail.setVisibility(View.GONE);
                    }
                }
            } else {
                tvStatusSaatIni.setVisibility(View.GONE);
                if (layoutStatusDetail != null) {
                    layoutStatusDetail.setVisibility(View.GONE);
                }
            }

            tvTahapSaatIni.setText(perkara.getTahapSaatIni() != null ? perkara.getTahapSaatIni() : "-");

            // Build tersangka list string
            List<Tersangka> tersangkaList = perkara.getTersangkas();
            if (tersangkaList != null && !tersangkaList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < tersangkaList.size(); i++) {
                    sb.append(i + 1).append(". ").append(tersangkaList.get(i).getNama());
                    if (i < tersangkaList.size() - 1) {
                        sb.append("\n");
                    }
                }
                tvTersangkas.setText(sb.toString());
            } else {
                tvTersangkas.setText("Belum ada data tersangka");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(perkara);
                }
            });
        }

        /**
         * Mengubah teks status panjang menjadi kode ringkas untuk pill badge.
         * Contoh:
         * - "P-20 (Pengembalian SPDP)" -> "P-20"
         * - "Terbitkan P-20"           -> "P-20"
         * - "P-21 (Berkas Lengkap)"    -> "P-21"
         */
        private static String getCompactStatus(String fullStatus) {
            if (fullStatus == null || fullStatus.trim().isEmpty()) {
                return "-";
            }
            String status = fullStatus.trim();

            // 1. Ekstrak kode P-xx / P-xxA (P-16, P-17, P-18, P-19, P-20, P-21, P-21A, dll)
            Matcher pMatcher = Pattern.compile("(?i)\\b(P-\\d+[a-z]?)\\b").matcher(status);
            if (pMatcher.find()) {
                return pMatcher.group(1).toUpperCase();
            }

            // 2. Ekstrak kode SOP FORM-xx
            Matcher formMatcher = Pattern.compile("(?i)\\b(FORM-?\\d+)\\b").matcher(status);
            if (formMatcher.find()) {
                return formMatcher.group(1).toUpperCase();
            }

            // 3. Tahap II / Tahap I
            String lower = status.toLowerCase();
            if (lower.contains("tahap ii") || lower.contains("tahap 2")) {
                return "Tahap II";
            }
            if (lower.contains("tahap i") || lower.contains("tahap 1")) {
                return "Tahap I";
            }

            // 4. SPDP
            if (lower.contains("spdp")) {
                return "SPDP";
            }

            // 5. Potong sebelum tanda kurung jika ada (misal: "Pemberitahuan (SPDP)" -> "Pemberitahuan")
            int parenIdx = status.indexOf('(');
            if (parenIdx > 0) {
                status = status.substring(0, parenIdx).trim();
            }

            // 6. Jika masih panjang (> 12 karakter), potong dengan elipsis
            if (status.length() > 12) {
                return status.substring(0, 11).trim() + "…";
            }

            return status;
        }

        /**
         * Menerapkan warna latar dan teks pill badge yang harmonis sesuai kode status.
         */
        private static void applyBadgeStyle(TextView badge, String compactStatus) {
            if (badge == null || compactStatus == null) return;

            String s = compactStatus.toUpperCase();
            int bgColor;
            int textColor;

            if (s.contains("P-20") || s.contains("FORM-3") || s.contains("FORM-7") || s.contains("OVERDUE")) {
                // Merah / Rose (Pemberhentian / Waktu Habis)
                bgColor   = Color.parseColor("#FEE2E2");
                textColor = Color.parseColor("#DC2626");
            } else if (s.contains("P-21") || s.contains("LENGKAP") || s.contains("SELESAI")) {
                // Hijau / Emerald (Berkas Lengkap / Berhasil)
                bgColor   = Color.parseColor("#DCFCE7");
                textColor = Color.parseColor("#16A34A");
            } else if (s.contains("P-18") || s.contains("P-19") || s.contains("BELUM")) {
                // Amber / Orange (Petunjuk / Belum Lengkap)
                bgColor   = Color.parseColor("#FEF3C7");
                textColor = Color.parseColor("#D97706");
            } else if (s.contains("P-17") || s.contains("FORM-2")) {
                // Biru / Sky Blue (Permintaan Perkembangan / Peringatan)
                bgColor   = Color.parseColor("#E0F2FE");
                textColor = Color.parseColor("#0284C7");
            } else if (s.contains("P-16") || s.contains("SPDP") || s.contains("KOORDINASI")) {
                // Teal (Awal / Penunjukan JPU)
                bgColor   = Color.parseColor("#CCFBF1");
                textColor = Color.parseColor("#0F766E");
            } else if (s.contains("TAHAP")) {
                // Ungu (Tahap Berkas)
                bgColor   = Color.parseColor("#F3E8FF");
                textColor = Color.parseColor("#7E22CE");
            } else {
                // Netral Slate
                bgColor   = Color.parseColor("#F1F5F9");
                textColor = Color.parseColor("#475569");
            }

            ViewCompat.setBackgroundTintList(badge, ColorStateList.valueOf(bgColor));
            badge.setTextColor(textColor);
        }
    }
}