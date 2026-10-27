package com.kejaksaan.reminder.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.model.NotificationResponse;
import com.kejaksaan.reminder.model.PushNotificationItem;
import com.kejaksaan.reminder.repository.NotificationRepository;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.ArrayList;
import java.util.List;

public class NotificationViewModel extends AndroidViewModel {

    private final NotificationRepository repository;

    private final MutableLiveData<List<PushNotificationItem>> notificationsList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private int currentPage = 1;
    private int lastPage = 1;
    private static final int PER_PAGE = 20;

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        repository = new NotificationRepository(application);
    }

    public LiveData<List<PushNotificationItem>> getNotificationsList() {
        return notificationsList;
    }

    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }

    public LiveData<Boolean> getIsLoadingMore() {
        return isLoadingMore;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public boolean hasMorePages() {
        return currentPage < lastPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Muat halaman pertama notifikasi (Refresh / Initial Load).
     */
    public void loadInitial() {
        isRefreshing.setValue(true);
        currentPage = 1;

        repository.getNotificationsPaged(1, PER_PAGE, new MutableLiveData<ApiResult<NotificationResponse.NotificationPagination>>() {
            @Override
            public void postValue(ApiResult<NotificationResponse.NotificationPagination> result) {
                if (result == null) return;

                if (result.isSuccess() && result.getData() != null) {
                    NotificationResponse.NotificationPagination pagination = result.getData();
                    currentPage = pagination.getCurrentPage();
                    lastPage = pagination.getLastPage();

                    List<PushNotificationItem> items = pagination.getData() != null ? pagination.getData() : new ArrayList<>();
                    notificationsList.postValue(new ArrayList<>(items));
                    isRefreshing.postValue(false);
                } else if (result.isError()) {
                    errorMessage.postValue(result.getMessage());
                    isRefreshing.postValue(false);
                }
            }
        });
    }

    /**
     * Muat halaman berikutnya (Pagination / Load More).
     */
    public void loadMore() {
        if (!hasMorePages() || Boolean.TRUE.equals(isLoadingMore.getValue()) || Boolean.TRUE.equals(isRefreshing.getValue())) {
            return;
        }

        isLoadingMore.setValue(true);
        int nextPage = currentPage + 1;

        repository.getNotificationsPaged(nextPage, PER_PAGE, new MutableLiveData<ApiResult<NotificationResponse.NotificationPagination>>() {
            @Override
            public void postValue(ApiResult<NotificationResponse.NotificationPagination> result) {
                if (result == null) return;

                if (result.isSuccess() && result.getData() != null) {
                    NotificationResponse.NotificationPagination pagination = result.getData();
                    currentPage = pagination.getCurrentPage();
                    lastPage = pagination.getLastPage();

                    List<PushNotificationItem> newItems = pagination.getData() != null ? pagination.getData() : new ArrayList<>();
                    List<PushNotificationItem> current = notificationsList.getValue();
                    if (current == null) current = new ArrayList<>();
                    current.addAll(newItems);

                    notificationsList.postValue(new ArrayList<>(current));
                    isLoadingMore.postValue(false);
                } else if (result.isError()) {
                    errorMessage.postValue(result.getMessage());
                    isLoadingMore.postValue(false);
                }
            }
        });
    }
}
