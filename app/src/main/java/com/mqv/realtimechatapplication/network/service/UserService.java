package com.mqv.realtimechatapplication.network.service;

import com.mqv.realtimechatapplication.data.model.LoggedInUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.CustomUser;
import com.mqv.realtimechatapplication.util.Const;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UserService {
    @FormUrlEncoded
    @POST(value = "/login")
    Observable<ApiResponse<LoggedInUser>> login(@Field("username") String username,
                                                 @Field("password") String password);

    @GET(value = "user/info")
    Observable<ApiResponse<CustomUser>> fetchCustomUserInfo(@Header(Const.AUTHORIZATION) String token,
                                                            @Header(Const.AUTHORIZER) String authorizer);
}
