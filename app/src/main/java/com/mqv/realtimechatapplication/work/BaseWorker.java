package com.mqv.realtimechatapplication.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.UUID;

public abstract class BaseWorker implements WorkerProperty {
    private final Context context;
    private final String workerName;
    private WorkRequest request;

    private static final String DEFAULT_WORK_NAME = "BASE_WORK_" + System.currentTimeMillis();

    public BaseWorker(Context context) {
        this(context, DEFAULT_WORK_NAME);
    }

    public BaseWorker(Context context, String workerName) {
        this.context = context;
        this.workerName = workerName;
    }

    @NonNull
    public abstract WorkRequest createRequest();

    public void run() {
        request = createRequest();
        WorkManager manager = WorkManager.getInstance(context);

        if (isUniqueWork()) {
            if (request instanceof OneTimeWorkRequest) {
                manager.enqueueUniqueWork(workerName, getOneTimeWorkPolicy(), (OneTimeWorkRequest) request);
            } else {
                manager.enqueueUniquePeriodicWork(workerName, getPeriodicWorkPolicy(), (PeriodicWorkRequest) request);
            }
        } else {
            manager.enqueue(request);
        }
    }

    private UUID getWorkId() {
        return request.getId();
    }

    public LiveData<WorkInfo> getWorkInfo() {
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(getWorkId());
    }
}
