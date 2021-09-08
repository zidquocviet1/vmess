package com.mqv.realtimechatapplication.network.service;

import com.mqv.realtimechatapplication.data.model.LoggedInUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Const;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface UserService {
    @FormUrlEncoded
    @POST(value = "/login")
    Observable<ApiResponse<LoggedInUser>> login(@Field("username") String username,
                                                @Field("password") String password);

    @FormUrlEncoded
    @POST(value = "user/add")
    Observable<Response<String>> addUser(@Field("uid") String uid);

    @GET
    @FormUrlEncoded
    Observable<ApiResponse<User>> fetchUserFromRemote(@Header(Const.AUTHORIZATION) String token,
                                                      @Header(Const.AUTHORIZER) String authorizer,
                                                      @Field("uid") String uid);

    @GET(value = "user/info")
    Observable<ApiResponse<User>> fetchCustomUserInfo(@Header(Const.AUTHORIZATION) String token,
                                                      @Header(Const.AUTHORIZER) String authorizer);

    @PUT(value = "user/upload-photo")
    @Multipart
    Observable<ApiResponse<String>> updateProfilePicture(@Header(Const.AUTHORIZATION) String token,
                                                         @Header(Const.AUTHORIZER) String authorizer,
                                                         @Part("type") RequestBody type,
                                                         @Part MultipartBody.Part part);
}
