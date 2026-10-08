package com.kejaksaan.reminder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.Tersangka;

import java.util.ArrayList;
import java.util.List;

public class PerkaraAdapter extends RecyclerView.Adapter<PerkaraAdapter.PerkaraViewHolder> {

    private final List<Perkara> perkaraList = new ArrayList<>();

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
        holder.bind(perkara);
    }

    @Override
    public int getItemCount() {
        return perkaraList.size();
    }

    static class PerkaraViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNomorPerkara;
        private final TextView tvStatusSaatIni;
        private final TextView tvTahapSaatIni;
        private final TextView tvTersangkas;

        public PerkaraViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomorPerkara = itemView.findViewById(R.id.tvNomorPerkara);
            tvStatusSaatIni = itemView.findViewById(R.id.tvStatusSaatIni);
            tvTahapSaatIni = itemView.findViewById(R.id.tvTahapSaatIni);
            tvTersangkas = itemView.findViewById(R.id.tvTersangkas);
        }

        public void bind(Perkara perkara) {
            tvNomorPerkara.setText(perkara.getNomorPerkara() != null ? perkara.getNomorPerkara() : "-");
            tvStatusSaatIni.setText(perkara.getStatusSaatIni() != null ? perkara.getStatusSaatIni() : "-");
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
        }
    }
}
