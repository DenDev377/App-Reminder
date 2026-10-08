package com.kejaksaan.reminder.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.model.Reminder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private Context context;
    private List<Reminder> reminderList = new ArrayList<>();

    public ReminderAdapter(Context context) {
        this.context = context;
    }

    public void setReminderList(List<Reminder> reminderList) {
        this.reminderList = reminderList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder_card, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        holder.tvReminderJenis.setText(reminder.getJenisReminder() != null ? reminder.getJenisReminder() : "-");
        holder.tvReminderStatus.setText(reminder.getStatusUrgensi() != null ? reminder.getStatusUrgensi() : "-");

        if (reminder.getPerkara() != null) {
            holder.tvReminderNomor.setText("Nomor: " + reminder.getPerkara().getNomorPerkara());
        } else {
            holder.tvReminderNomor.setText("Nomor: -");
        }

        holder.tvReminderTahap.setText(reminder.getTahap() != null ? reminder.getTahap() : "-");
        holder.tvReminderDeadline.setText(formatDate(reminder.getDeadline()));
        holder.tvReminderSisaHari.setText("Sisa: " + reminder.getSisaHari() + " Hari");

        // Ganti warna berdasarkan urgensi (contoh sederhana)
        String status = reminder.getStatusUrgensi();
        if (status != null) {
            if (status.equalsIgnoreCase("Jatuh Tempo") || status.toLowerCase().contains("overdue")) {
                holder.tvReminderStatus.getBackground().setTint(Color.parseColor("#EF4444")); // Merah
                holder.tvReminderStatus.setTextColor(Color.WHITE);
                holder.tvReminderSisaHari.setTextColor(Color.parseColor("#DC2626"));
            } else {
                holder.tvReminderStatus.getBackground().setTint(Color.parseColor("#F59E0B")); // Oranye/Amber
                holder.tvReminderStatus.setTextColor(Color.WHITE);
                holder.tvReminderSisaHari.setTextColor(Color.parseColor("#D97706"));
            }
        }
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    private String formatDate(String dateString) {
        if (dateString == null) return "-";
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
            Date date = sdfInput.parse(dateString);
            SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
            return sdfOutput.format(date);
        } catch (ParseException e) {
            try {
                SimpleDateFormat sdfInput2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                Date date = sdfInput2.parse(dateString);
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
                return sdfOutput.format(date);
            } catch (ParseException ex) {
                return dateString;
            }
        }
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvReminderJenis, tvReminderStatus, tvReminderNomor, tvReminderTahap, tvReminderDeadline, tvReminderSisaHari;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReminderJenis = itemView.findViewById(R.id.tvReminderJenis);
            tvReminderStatus = itemView.findViewById(R.id.tvReminderStatus);
            tvReminderNomor = itemView.findViewById(R.id.tvReminderNomor);
            tvReminderTahap = itemView.findViewById(R.id.tvReminderTahap);
            tvReminderDeadline = itemView.findViewById(R.id.tvReminderDeadline);
            tvReminderSisaHari = itemView.findViewById(R.id.tvReminderSisaHari);
        }
    }
}
