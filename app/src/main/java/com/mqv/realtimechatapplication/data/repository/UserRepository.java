package com.mqv.realtimechatapplication.data.repository;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;

public interface UserRepository {
    Observable<Response<String>> addUser(String uid);
}
