package com.mqv.vmess.util;

public class Resource<T> {
    private NetworkStatus status;
    private T data;
    private String message;

    private Resource(NetworkStatus status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public NetworkStatus getStatus() {
        return status;
    }

    public void setStatus(NetworkStatus status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static <T> Resource<T> Success(T data){
        return new Resource<>(NetworkStatus.SUCCESS, data, null);
    }

    public static <T> Resource<T> Loading(){
        return new Resource<>(NetworkStatus.LOADING, null, null);
    }

    public static <T> Resource<T> Error(String message){
        return new Resource<>(NetworkStatus.ERROR, null, message);
    }
}
