package com.kejaksaan.reminder.util;

/**
 * Generic wrapper untuk hasil operasi Repository.
 * Digunakan sebagai nilai yang di-post ke LiveData.
 *
 * Contoh pemakaian di ViewModel:
 *   MutableLiveData<ApiResult<User>> userLiveData = new MutableLiveData<>();
 *   userLiveData.postValue(ApiResult.loading());
 *   ...
 *   userLiveData.postValue(ApiResult.success(user));
 *   userLiveData.postValue(ApiResult.error("Pesan error"));
 */
public class ApiResult<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    private final Status status;
    private final T data;
    private final String message;

    private ApiResult(Status status, T data, String message) {
        this.status  = status;
        this.data    = data;
        this.message = message;
    }

    // ─── Factory methods ─────────────────────────────────────────────────────────

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(Status.SUCCESS, data, null);
    }

    public static <T> ApiResult<T> error(String msg) {
        return new ApiResult<>(Status.ERROR, null, msg);
    }

    public static <T> ApiResult<T> loading() {
        return new ApiResult<>(Status.LOADING, null, null);
    }

    // ─── Getters ─────────────────────────────────────────────────────────────────

    public Status getStatus()   { return status; }
    public T getData()          { return data; }
    public String getMessage()  { return message; }

    public boolean isSuccess()  { return status == Status.SUCCESS; }
    public boolean isError()    { return status == Status.ERROR; }
    public boolean isLoading()  { return status == Status.LOADING; }
}
