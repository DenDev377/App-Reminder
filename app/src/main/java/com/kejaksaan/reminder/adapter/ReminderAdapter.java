package com.kejaksaan.reminder.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.model.Reminder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
    }

    private final Context context;
    private final boolean isHorizontal;
    private final List<Reminder> reminderList = new ArrayList<>();
    private final Set<Integer> reminderIds = new HashSet<>();
    private OnReminderClickListener onReminderClickListener;

    public ReminderAdapter(Context context) {
        this(context, false);
    }

    public ReminderAdapter(Context context, boolean isHorizontal) {
        this.context = context;
        this.isHorizontal = isHorizontal;
    }

    public void setOnReminderClickListener(OnReminderClickListener listener) {
        this.onReminderClickListener = listener;
    }

    /**
     * Mengganti seluruh data reminder (biasanya saat filter berubah atau refresh).
     */
    public void setReminderList(List<Reminder> list) {
        this.reminderList.clear();
        this.reminderIds.clear();
        if (list != null) {
            for (Reminder r : list) {
                if (r != null && !reminderIds.contains(r.getId())) {
                    reminderIds.add(r.getId());
                    this.reminderList.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Menambahkan item baru tanpa duplikasi (untuk pagination / infinite scroll).
     */
    public void appendReminders(List<Reminder> newItems) {
        if (newItems == null || newItems.isEmpty()) return;

        int startPosition = this.reminderList.size();
        int addedCount = 0;

        for (Reminder r : newItems) {
            if (r != null && !reminderIds.contains(r.getId())) {
                reminderIds.add(r.getId());
                this.reminderList.add(r);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            notifyItemRangeInserted(startPosition, addedCount);
        }
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_reminder_horizontal_card : R.layout.item_reminder_card;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);
        holder.bind(reminder);
        holder.itemView.setOnClickListener(v -> {
            if (onReminderClickListener != null) {
                onReminderClickListener.onReminderClick(reminder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    private static String formatDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return "-";
        try {
            String datePart = dateString.trim();
            if (datePart.contains("T")) {
                datePart = datePart.substring(0, datePart.indexOf("T")).trim();
            }
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfInput.parse(datePart);
            if (date != null) {
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
                return sdfOutput.format(date);
            }
        } catch (Exception ignored) {
        }
        return dateString;
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardReminder;
        ImageView imgReminderIcon;
        TextView tvReminderJenis;
        TextView tvReminderStatus;
        TextView tvReminderNomor;
        TextView tvReminderTersangka;
        TextView tvReminderTahap;
        TextView tvReminderDeadline;
        TextView tvReminderSisaHari;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardReminder        = itemView.findViewById(R.id.cardReminder);
            imgReminderIcon     = itemView.findViewById(R.id.imgReminderIcon);
            tvReminderJenis     = itemView.findViewById(R.id.tvReminderJenis);
            tvReminderStatus    = itemView.findViewById(R.id.tvReminderStatus);
            tvReminderNomor     = itemView.findViewById(R.id.tvReminderNomor);
            tvReminderTersangka = itemView.findViewById(R.id.tvReminderTersangka);
            tvReminderTahap     = itemView.findViewById(R.id.tvReminderTahap);
            tvReminderDeadline  = itemView.findViewById(R.id.tvReminderDeadline);
            tvReminderSisaHari  = itemView.findViewById(R.id.tvReminderSisaHari);
        }

        public void bind(Reminder reminder) {
            tvReminderJenis.setText(reminder.getJenisReminder() != null ? reminder.getJenisReminder() : "Reminder");

            String status = reminder.getStatusUrgensi() != null ? reminder.getStatusUrgensi() : "-";
            tvReminderStatus.setText(status);

            if (reminder.getPerkara() != null && reminder.getPerkara().getNomorPerkara() != null) {
                tvReminderNomor.setText("Nomor: " + reminder.getPerkara().getNomorPerkara());
            } else {
                tvReminderNomor.setText("Nomor: -");
            }

            // Suspect name(s)
            String tersangka = reminder.getTersangkaNames();
            tvReminderTersangka.setText("Tersangka: " + tersangka);

            // Stage
            String tahap = reminder.getTahap() != null ? reminder.getTahap() : "-";
            tvReminderTahap.setText("Tahap: " + tahap);

            // Deadline
            tvReminderDeadline.setText(formatDate(reminder.getDeadline()));

            // Sisa Hari (dihitung dinamis dari deadline)
            int sisa = reminder.getSisaHari();
            if (sisa < 0) {
                tvReminderSisaHari.setText("Lewat " + Math.abs(sisa) + " Hari");
            } else if (sisa == 0) {
                tvReminderSisaHari.setText("Jatuh Tempo Hari Ini");
            } else {
                tvReminderSisaHari.setText("Sisa " + sisa + " Hari");
            }

            // Styling based on Urgensi
            applyUrgensiStyle(status);
        }

        private void applyUrgensiStyle(String status) {
            int badgeColor;
            int strokeColor;
            int textColor;

            if (status.equalsIgnoreCase("Overdue") || status.equalsIgnoreCase("Jatuh Tempo")) {
                badgeColor  = Color.parseColor("#EF4444"); // Red
                strokeColor = Color.parseColor("#FECACA");
                textColor   = Color.parseColor("#DC2626");
            } else if (status.equalsIgnoreCase("H-3")) {
                badgeColor  = Color.parseColor("#F59E0B"); // Amber
                strokeColor = Color.parseColor("#FDE68A");
                textColor   = Color.parseColor("#D97706");
            } else if (status.equalsIgnoreCase("Aman")) {
                badgeColor  = Color.parseColor("#10B981"); // Emerald
                strokeColor = Color.parseColor("#A7F3D0");
                textColor   = Color.parseColor("#059669");
            } else {
                badgeColor  = Color.parseColor("#64748B"); // Slate
                strokeColor = Color.parseColor("#E2E8F0");
                textColor   = Color.parseColor("#475569");
            }

            tvReminderStatus.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
            tvReminderStatus.setTextColor(Color.WHITE);

            imgReminderIcon.setImageTintList(ColorStateList.valueOf(textColor));
            tvReminderJenis.setTextColor(textColor);
            tvReminderSisaHari.setTextColor(textColor);

            if (cardReminder != null) {
                cardReminder.setStrokeColor(strokeColor);
            }
        }
    }
}