package com.kejaksaan.reminder.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PushNotificationItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayNotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnNotificationActionListener {
        void onPerkaraClick(int perkaraId, String nomorPerkara);
        void onToggleEarlierClick();
        void onLoadMoreClick();
    }

    // ─── Feed Item Representation ────────────────────────────────────────────────
    public static class FeedItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_PUSH = 1;
        public static final int TYPE_TOGGLE_EARLIER = 2;
        public static final int TYPE_LOAD_MORE = 3;
        public static final int TYPE_EMPTY = 4;

        public final int type;
        public String title;
        public int count;
        public int iconRes;
        public int iconColor;

        public PushNotificationItem pushItem;
        public String emptySubtitle;

        public boolean isExpanded;
        public boolean isLoading;

        public FeedItem(int type) {
            this.type = type;
        }

        public static FeedItem createHeader(String title, int count, int iconRes, int iconColor) {
            FeedItem item = new FeedItem(TYPE_HEADER);
            item.title = title;
            item.count = count;
            item.iconRes = iconRes;
            item.iconColor = iconColor;
            return item;
        }

        public static FeedItem createPush(PushNotificationItem pushItem) {
            FeedItem item = new FeedItem(TYPE_PUSH);
            item.pushItem = pushItem;
            return item;
        }

        public static FeedItem createToggleEarlier(String title, boolean isExpanded) {
            FeedItem item = new FeedItem(TYPE_TOGGLE_EARLIER);
            item.title = title;
            item.isExpanded = isExpanded;
            return item;
        }

        public static FeedItem createLoadMore(boolean isLoading) {
            FeedItem item = new FeedItem(TYPE_LOAD_MORE);
            item.isLoading = isLoading;
            return item;
        }

        public static FeedItem createEmpty(String title, String subtitle) {
            FeedItem item = new FeedItem(TYPE_EMPTY);
            item.title = title;
            item.emptySubtitle = subtitle;
            return item;
        }
    }

    private final Context context;
    private final OnNotificationActionListener listener;
    private final List<FeedItem> displayItems = new ArrayList<>();

    public TodayNotificationAdapter(Context context, OnNotificationActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<FeedItem> items) {
        displayItems.clear();
        if (items != null) {
            displayItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case FeedItem.TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.item_notification_section_header, parent, false));
            case FeedItem.TYPE_PUSH:
                return new PushViewHolder(inflater.inflate(R.layout.item_notification_push_card, parent, false));
            case FeedItem.TYPE_TOGGLE_EARLIER:
                return new ToggleEarlierViewHolder(inflater.inflate(R.layout.item_notification_toggle_earlier, parent, false));
            case FeedItem.TYPE_LOAD_MORE:
                return new LoadMoreViewHolder(inflater.inflate(R.layout.item_notification_load_more, parent, false));
            case FeedItem.TYPE_EMPTY:
            default:
                return new EmptyViewHolder(inflater.inflate(R.layout.item_notification_empty_card, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FeedItem item = displayItems.get(position);
        switch (item.type) {
            case FeedItem.TYPE_HEADER:
                ((HeaderViewHolder) holder).bind(item);
                break;
            case FeedItem.TYPE_PUSH:
                ((PushViewHolder) holder).bind(item.pushItem, listener);
                break;
            case FeedItem.TYPE_TOGGLE_EARLIER:
                ((ToggleEarlierViewHolder) holder).bind(item, listener);
                break;
            case FeedItem.TYPE_LOAD_MORE:
                ((LoadMoreViewHolder) holder).bind(item, listener);
                break;
            case FeedItem.TYPE_EMPTY:
                ((EmptyViewHolder) holder).bind(item);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    // ─── ViewHolders ─────────────────────────────────────────────────────────────

    // 1. Header Section ViewHolder
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgIcon;
        private final TextView tvTitle;
        private final TextView tvCount;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgSectionIcon);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
            tvCount = itemView.findViewById(R.id.tvSectionCount);
        }

        void bind(FeedItem item) {
            if (item == null) return;
            tvTitle.setText(item.title != null ? item.title : "");
            tvCount.setText(String.valueOf(item.count));

            if (item.iconRes != 0) {
                imgIcon.setImageResource(item.iconRes);
            }
            if (item.iconColor != 0) {
                imgIcon.setColorFilter(item.iconColor);
                tvCount.setTextColor(item.iconColor);
            }
        }
    }

    // 2. Push Notification Tile ViewHolder
    static class PushViewHolder extends RecyclerView.ViewHolder {
        private final View itemViewRoot;
        private final FrameLayout layoutIconContainer;
        private final ImageView imgPushIcon;
        private final TextView tvTitle;
        private final TextView tvBadge;
        private final TextView tvBody;
        private final TextView tvPerkaraRef;
        private final TextView tvTime;

        PushViewHolder(@NonNull View itemView) {
            super(itemView);
            itemViewRoot = itemView.findViewById(R.id.cardPushNotification);
            layoutIconContainer = itemView.findViewById(R.id.layoutIconContainer);
            imgPushIcon = itemView.findViewById(R.id.imgPushIcon);
            tvTitle = itemView.findViewById(R.id.tvPushTitle);
            tvBadge = itemView.findViewById(R.id.tvPushBadge);
            tvBody = itemView.findViewById(R.id.tvPushBody);
            tvPerkaraRef = itemView.findViewById(R.id.tvPushPerkaraRef);
            tvTime = itemView.findViewById(R.id.tvPushTime);
        }

        void bind(PushNotificationItem item, OnNotificationActionListener listener) {
            if (item == null) return;

            tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Pemberitahuan Sistem");
            tvBody.setText(item.getBody() != null ? item.getBody() : "");

            String badgeText = item.getHumanEventTypeLabel();
            tvBadge.setText(badgeText);

            String event = item.getEventType() != null ? item.getEventType().toUpperCase(Locale.ROOT) : "";
            int iconBgColor;
            int iconColor;
            int badgeBgColor;
            int badgeTextColor;

            if (event.contains("STAGE") || event.contains("PROGRES") || event.contains("TAHAP")) {
                iconBgColor = Color.parseColor("#ECFDF5");
                iconColor = Color.parseColor("#059669");
                badgeBgColor = Color.parseColor("#D1FAE5");
                badgeTextColor = Color.parseColor("#065F46");
                imgPushIcon.setImageResource(R.drawable.ic_check_circle);
            } else if (event.contains("H0") || event.contains("OVERDUE")) {
                iconBgColor = Color.parseColor("#FEF2F2");
                iconColor = Color.parseColor("#DC2626");
                badgeBgColor = Color.parseColor("#FEE2E2");
                badgeTextColor = Color.parseColor("#991B1B");
                imgPushIcon.setImageResource(R.drawable.ic_alarm);
            } else if (event.contains("H1") || event.contains("H2")) {
                iconBgColor = Color.parseColor("#FFFBEB");
                iconColor = Color.parseColor("#D97706");
                badgeBgColor = Color.parseColor("#FEF3C7");
                badgeTextColor = Color.parseColor("#92400E");
                imgPushIcon.setImageResource(R.drawable.ic_alarm);
            } else if (event.contains("H3")) {
                iconBgColor = Color.parseColor("#FEFCE8");
                iconColor = Color.parseColor("#CA8A04");
                badgeBgColor = Color.parseColor("#FEF9C3");
                badgeTextColor = Color.parseColor("#854D0E");
                imgPushIcon.setImageResource(R.drawable.ic_notification);
            } else if (event.contains("PERKARA") || event.contains("DISPOSISI")) {
                iconBgColor = Color.parseColor("#E6FFFA");
                iconColor = Color.parseColor("#00897B");
                badgeBgColor = Color.parseColor("#CCFBF1");
                badgeTextColor = Color.parseColor("#115E59");
                imgPushIcon.setImageResource(R.drawable.ic_perkara);
            } else {
                iconBgColor = Color.parseColor("#F0F9FF");
                iconColor = Color.parseColor("#0284C7");
                badgeBgColor = Color.parseColor("#E0F2FE");
                badgeTextColor = Color.parseColor("#075985");
                imgPushIcon.setImageResource(R.drawable.ic_notification);
            }

            layoutIconContainer.getBackground().setTint(iconBgColor);
            imgPushIcon.setColorFilter(iconColor);
            tvBadge.getBackground().setTint(badgeBgColor);
            tvBadge.setTextColor(badgeTextColor);

            tvTime.setText(formatDateTime(item));

            Perkara p = item.getPerkara();
            int perkaraId = (item.getPerkaraId() != null) ? item.getPerkaraId() : (p != null ? p.getId() : 0);

            if (p != null && p.getNomorPerkara() != null && !p.getNomorPerkara().isEmpty()) {
                tvPerkaraRef.setVisibility(View.VISIBLE);
                tvPerkaraRef.setText("Terkait: " + p.getNomorPerkara());
            } else {
                tvPerkaraRef.setVisibility(View.GONE);
            }

            if (perkaraId > 0) {
                itemViewRoot.setOnClickListener(v -> {
                    if (listener != null) {
                        String noPerkara = (p != null && p.getNomorPerkara() != null) ? p.getNomorPerkara() : "";
                        listener.onPerkaraClick(perkaraId, noPerkara);
                    }
                });
            } else {
                itemViewRoot.setOnClickListener(null);
            }
        }

        private String formatDateTime(PushNotificationItem item) {
            Date date = item.getParsedDate();
            if (date != null) {
                long now = System.currentTimeMillis();
                long diff = now - date.getTime();

                if (diff >= 0 && diff < 60 * 1000) {
                    return "Baru saja";
                }
                if (diff >= 0 && diff < 60 * 60 * 1000) {
                    long minutes = diff / (60 * 1000);
                    return minutes + " menit lalu";
                }

                Calendar itemCal = Calendar.getInstance();
                itemCal.setTime(date);

                Calendar todayCal = Calendar.getInstance();
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                if (itemCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                        && itemCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                    return "Hari ini, " + timeFormat.format(date) + " WIB";
                }

                todayCal.add(Calendar.DAY_OF_YEAR, -1);
                if (itemCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                        && itemCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                    return "Kemarin, " + timeFormat.format(date) + " WIB";
                }

                SimpleDateFormat fullFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
                return fullFormat.format(date) + " WIB";
            }
            return item.getCreatedAt() != null ? item.getCreatedAt() : "Baru saja";
        }
    }

    // 3. Toggle Earlier Notifications ViewHolder
    static class ToggleEarlierViewHolder extends RecyclerView.ViewHolder {
        private final View btnToggle;
        private final ImageView imgToggle;
        private final TextView tvToggle;

        ToggleEarlierViewHolder(@NonNull View itemView) {
            super(itemView);
            btnToggle = itemView.findViewById(R.id.btnToggleEarlier);
            imgToggle = itemView.findViewById(R.id.imgToggleIcon);
            tvToggle = itemView.findViewById(R.id.tvToggleText);
        }

        void bind(FeedItem item, OnNotificationActionListener listener) {
            if (item == null) return;
            tvToggle.setText(item.title != null ? item.title : "Notifikasi Sebelumnya");
            imgToggle.setImageResource(item.isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);

            btnToggle.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleEarlierClick();
                }
            });
        }
    }

    // 4. Load More Pagination ViewHolder
    static class LoadMoreViewHolder extends RecyclerView.ViewHolder {
        private final View btnLoadMore;
        private final ProgressBar pbLoadMore;
        private final TextView tvLoadMoreText;

        LoadMoreViewHolder(@NonNull View itemView) {
            super(itemView);
            btnLoadMore = itemView.findViewById(R.id.btnLoadMore);
            pbLoadMore = itemView.findViewById(R.id.pbLoadMore);
            tvLoadMoreText = itemView.findViewById(R.id.tvLoadMoreText);
        }

        void bind(FeedItem item, OnNotificationActionListener listener) {
            if (item == null) return;
            pbLoadMore.setVisibility(item.isLoading ? View.VISIBLE : View.GONE);
            tvLoadMoreText.setText(item.isLoading ? "Memuat..." : "Muat Notifikasi Lebih Lama...");

            btnLoadMore.setOnClickListener(v -> {
                if (listener != null && !item.isLoading) {
                    listener.onLoadMoreClick();
                }
            });
        }
    }

    // 5. Empty State ViewHolder
    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEmptyTitle;
        private final TextView tvEmptySubtitle;

        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmptyTitle = itemView.findViewById(R.id.tvEmptyTitle);
            tvEmptySubtitle = itemView.findViewById(R.id.tvEmptySubtitle);
        }

        void bind(FeedItem item) {
            if (item.title != null) tvEmptyTitle.setText(item.title);
            if (item.emptySubtitle != null) tvEmptySubtitle.setText(item.emptySubtitle);
        }
    }
}
