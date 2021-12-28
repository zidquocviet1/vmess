package com.mqv.realtimechatapplication.work;

import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;

public final class WorkDependency {
    public static void enqueue(BaseWorker worker) {
        worker.run();
    }

    public static LiveData<WorkInfo> enqueueAndGet(BaseWorker worker) {
        worker.run();
        return worker.getWorkInfo();
    }
}
