package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model untuk satu item riwayat push notification dari endpoint GET /api/notifications.
 */
public class PushNotificationItem {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("perkara_id")
    private Integer perkaraId;

    @SerializedName("perkara_reminder_id")
    private Integer perkaraReminderId;

    @SerializedName("event_type")
    private String eventType;

    @SerializedName("idempotency_key")
    private String idempotencyKey;

    @SerializedName("title")
    private String title;

    @SerializedName("body")
    private String body;

    @SerializedName("status")
    private String status;

    @SerializedName("error_message")
    private String errorMessage;

    @SerializedName("sent_at")
    private String sentAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("perkara")
    private Perkara perkara;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public Integer getPerkaraId() {
        return perkaraId;
    }

    public Integer getPerkaraReminderId() {
        return perkaraReminderId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSentAt() {
        return sentAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Perkara getPerkara() {
        return perkara;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    public java.util.Date getParsedDate() {
        if (createdAt == null || createdAt.trim().isEmpty()) return null;
        try {
            String s = createdAt.trim();
            if (s.contains(".")) {
                int dotIdx = s.indexOf('.');
                int endIdx = s.length();
                for (int i = dotIdx + 1; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c == 'Z' || c == '+' || c == '-') {
                        endIdx = i;
                        break;
                    }
                }
                s = s.substring(0, dotIdx) + (endIdx < s.length() ? s.substring(endIdx) : "");
            }
            s = s.replace("Z", "+0000").replace("+00:00", "+0000");

            java.text.SimpleDateFormat sdf;
            if (s.contains("T")) {
                if (s.contains("+") || (s.indexOf("T") != -1 && s.indexOf("-", s.indexOf("T")) != -1)) {
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault());
                } else {
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                }
            } else {
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            }
            return sdf.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isToday() {
        java.util.Date d = getParsedDate();
        if (d == null) return false;
        java.util.Calendar itemCal = java.util.Calendar.getInstance();
        itemCal.setTime(d);
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        return itemCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR)
                && itemCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR);
    }

    public String getHumanEventTypeLabel() {
        if (eventType == null) return "INFO";
        switch (eventType.toUpperCase(java.util.Locale.ROOT)) {
            case "STAGE_TRANSITION":
            case "TAHAP_TRANSITION":
            case "PERKARA_PROGRES":
                return "PROGRES TAHAP";
            case "REMINDER_H0":
                return "HARI INI (H-0)";
            case "REMINDER_OVERDUE":
                return "OVERDUE";
            case "REMINDER_H1":
                return "H-1 TEMPO";
            case "REMINDER_H2":
                return "H-2 TEMPO";
            case "REMINDER_H3":
                return "H-3 PENGINGAT";
            case "AUTO_TRANSITION_KOORDINASI":
                return "FORM-1B/1C TERBIT";
            case "AUTO_TRANSITION_P17":
                return "P-17 TERBIT";
            case "AUTO_TRANSITION_FORM2":
                return "FORM-2 TERBIT";
            case "AUTO_TRANSITION_FORM3":
                return "FORM-3 TERBIT";
            case "AUTO_TRANSITION_P20":
                return "P-20 TERBIT";
            case "AUTO_TRANSITION_FORM7":
                return "FORM-7 TERBIT";
            case "DISPOSISI":
            case "PERKARA_BARU":
            case "NEW_PERKARA":
                return "PERKARA BARU";
            default:
                return eventType.replace("_", " ").toUpperCase(java.util.Locale.ROOT);
        }
    }
}
