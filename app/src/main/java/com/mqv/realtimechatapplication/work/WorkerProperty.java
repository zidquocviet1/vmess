package com.mqv.realtimechatapplication.work;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;

public interface WorkerProperty {
    Constraints retrieveConstraint();

    boolean isUniqueWork();

    @NonNull
    default ExistingWorkPolicy getOneTimeWorkPolicy() {
        return ExistingWorkPolicy.REPLACE;
    }

    @NonNull
    default ExistingPeriodicWorkPolicy getPeriodicWorkPolicy() {
        return ExistingPeriodicWorkPolicy.REPLACE;
    }
}
