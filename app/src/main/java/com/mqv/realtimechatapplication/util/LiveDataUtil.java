package com.mqv.realtimechatapplication.util;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

public final class LiveDataUtil {
    // Custom with the additional function validator of two value
    public static <T> LiveData<T> distinctUntilChanged(LiveData<T> source, Validator<T> validator) {
        final MediatorLiveData<T> mediator = new MediatorLiveData<>();
        mediator.addSource(source, new Observer<>() {
            boolean mFirstTime = true;

            @Override
            public void onChanged(T currentValue) {
                final T previousValue = mediator.getValue();
                if (mFirstTime
                        || (previousValue == null && currentValue != null)
                        || (previousValue != null && !validator.areContentsTheSame(previousValue, currentValue))) {
                    mFirstTime = false;
                    mediator.setValue(currentValue);
                }
            }
        });
        return mediator;
    }

    public interface Validator<T> {
        boolean areContentsTheSame(T previousValue, T currentValue);
    }
}
