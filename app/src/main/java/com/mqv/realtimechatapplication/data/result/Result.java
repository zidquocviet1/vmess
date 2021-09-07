package com.mqv.realtimechatapplication.data.result;

import com.mqv.realtimechatapplication.util.NetworkStatus;

public class Result<T> {
    private final T success;
    private final Integer error;
    private final NetworkStatus status;

    public Result(NetworkStatus status, T success, Integer error) {
        this.status = status;
        this.success = success;
        this.error = error;
    }

    public static <T> Result<T> Fail(Integer error) {
        return new Result<>(NetworkStatus.ERROR, null, error);
    }

    public static <T> Result<T> Success(T data) {
        return new Result<>(NetworkStatus.SUCCESS, data, null);
    }

    public static <T> Result<T> Loading(){
        return new Result<>(NetworkStatus.LOADING, null, null);
    }

    public NetworkStatus getStatus() {
        return status;
    }

    public Integer getError() {
        return error;
    }

    public T getSuccess() {
        return success;
    }
}
