package com.mqv.vmess.network;

import com.google.gson.annotations.SerializedName;

public final class ApiResponse<T> {
    private T success;
    private String error;
    @SerializedName("status_code")
    private int statusCode;

    public ApiResponse(){}

    public ApiResponse(T success, String error, int statusCode) {
        this.success = success;
        this.error = error;
        this.statusCode = statusCode;
    }

    public T getSuccess() {
        return success;
    }

    public void setSuccess(T success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
