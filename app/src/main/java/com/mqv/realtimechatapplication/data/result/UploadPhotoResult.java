package com.mqv.realtimechatapplication.data.result;

import com.mqv.realtimechatapplication.util.NetworkStatus;

public class UploadPhotoResult {
    private final NetworkStatus status;
    private final String success;
    private final Integer error;

    public UploadPhotoResult(NetworkStatus status, String success, Integer error) {
        this.status = status;
        this.success = success;
        this.error = error;
    }

    public static UploadPhotoResult Fail(Integer error) {
        return new UploadPhotoResult(NetworkStatus.ERROR, null, error);
    }

    public static UploadPhotoResult Success(String msg) {
        return new UploadPhotoResult(NetworkStatus.SUCCESS, msg, null);
    }

    public static UploadPhotoResult Loading(){
        return new UploadPhotoResult(NetworkStatus.LOADING, null, null);
    }

    public NetworkStatus getStatus() {
        return status;
    }

    public Integer getError() {
        return error;
    }

    public String getSuccess() {
        return success;
    }
}
