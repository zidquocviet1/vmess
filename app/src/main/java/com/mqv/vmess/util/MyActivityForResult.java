package com.mqv.vmess.util;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyActivityForResult<I, O> {
    private OnActivityForResult<O> callback;
    private final ActivityResultLauncher<I> mLauncher;

    public interface OnActivityForResult<O> {
        void onResult(O result);
    }

    private MyActivityForResult(@NonNull ActivityResultCaller caller,
                                @NonNull ActivityResultContract<I, O> contract,
                                       @Nullable OnActivityForResult<O> callback) {
        this.callback = callback;
        this.mLauncher = caller.registerForActivityResult(contract, this::callOnActivityResult);
    }

    public static <I, O> MyActivityForResult<I, O> registerActivityForResult(@NonNull ActivityResultCaller caller,
                                                                             @NonNull ActivityResultContract<I, O> contract,
                                                                             @Nullable OnActivityForResult<O> callback) {
        return new MyActivityForResult<>(caller, contract, callback);
    }

    public static <I, O> MyActivityForResult<I, O> registerActivityForResult(@NonNull ActivityResultCaller caller,
                                                                             @NonNull ActivityResultContract<I, O> contract) {
        return new MyActivityForResult<>(caller, contract, null);
    }

    public void launch(I input, OnActivityForResult<O> callback){
        if (callback != null){
            this.callback = callback;
        }
        mLauncher.launch(input);
    }

    public void launch(I input){
        this.launch(input, this.callback);
    }

    private void callOnActivityResult(O result){
        if (callback != null){
            callback.onResult(result);
        }
    }
}
