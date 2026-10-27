package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Wrapper respons untuk GET /api/perkara/{id}/available-actions.
 */
public class AvailableActionsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("can_execute")
    private boolean canExecute;

    @SerializedName("perkara")
    private Perkara perkara;

    @SerializedName("data")
    private List<WorkflowAction> data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCanExecute() {
        return canExecute;
    }

    public void setCanExecute(boolean canExecute) {
        this.canExecute = canExecute;
    }

    public Perkara getPerkara() {
        return perkara;
    }

    public void setPerkara(Perkara perkara) {
        this.perkara = perkara;
    }

    public List<WorkflowAction> getData() {
        return data;
    }

    public void setData(List<WorkflowAction> data) {
        this.data = data;
    }
}
