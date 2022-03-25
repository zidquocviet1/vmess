package com.mqv.vmess.util;

import android.util.Pair;

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

    public static <A, B> LiveData<Pair<A, B>> zip(LiveData<A> source1, LiveData<B> source2) {
        final MediatorLiveData<Pair<A, B>> mediator = new MediatorLiveData<>();

        mediator.addSource(source1, a -> {
            if (a != null && source2.getValue() != null) {
                mediator.setValue(Pair.create(a, source2.getValue()));
            }
        });

        mediator.addSource(source2, b -> {
            if (b != null && source1.getValue() != null) {
                mediator.setValue(Pair.create(source1.getValue(), b));
            }
        });

        return mediator;
    }

    public interface Validator<T> {
        boolean areContentsTheSame(T previousValue, T currentValue);
    }
}
