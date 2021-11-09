package com.mqv.realtimechatapplication.work;

public final class WorkDependency {
    public static void enqueue(BaseWorker worker) {
        worker.run();
    }
}
