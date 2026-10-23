package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model request untuk pendaftaran (POST) dan pencabutan (DELETE) token perangkat FCM
 * pada endpoint /api/device-token.
 */
public class DeviceTokenRequest {

    @SerializedName("fcm_token")
    private String fcmToken;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("device_type")
    private String deviceType;

    public DeviceTokenRequest() {
    }

    public DeviceTokenRequest(String fcmToken, String deviceId, String deviceType) {
        this.fcmToken = fcmToken;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
    }

    public DeviceTokenRequest(String fcmToken, String deviceId) {
        this.fcmToken = fcmToken;
        this.deviceId = deviceId;
        this.deviceType = "android";
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
}
