package com.mqv.realtimechatapplication.work;

import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;

import com.google.common.util.concurrent.ListenableFuture;

public final class WorkDependency {
    public static void enqueue(BaseWorker worker) {
        worker.run();
    }

    // Use on main thread
    public static LiveData<WorkInfo> enqueueAndGetLiveData(BaseWorker worker) {
        worker.run();
        return worker.getWorkInfoLiveData();
    }

    // Use on background thread
    public static ListenableFuture<WorkInfo> enqueueAndGet(BaseWorker worker) {
        worker.run();
        return worker.getWorkInfo();
    }
}
