package com.mqv.realtimechatapplication.network.service;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Const;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface UserService {
    @POST(value = "user/login")
    Observable<ApiResponse<User>> loginWithToken(@Header(Const.AUTHORIZATION) String token,
                                                 @Header(Const.AUTHORIZER) String authorizer);

    @GET(value = "user/info")
    Observable<ApiResponse<User>> fetchUserFromRemote(@Header(Const.AUTHORIZATION) String token,
                                                      @Header(Const.AUTHORIZER) String authorizer,
                                                      @Query("uid") String uid);

    @GET(value = "user/info")
    Observable<ApiResponse<User>> fetchCustomUserInfo(@Header(Const.AUTHORIZATION) String token,
                                                      @Header(Const.AUTHORIZER) String authorizer);

    @PUT(value = "user/upload-photo")
    @Multipart
    Observable<ApiResponse<String>> updateProfilePicture(@Header(Const.AUTHORIZATION) String token,
                                                         @Header(Const.AUTHORIZER) String authorizer,
                                                         @Part("type") RequestBody type,
                                                         @Part MultipartBody.Part part);

    @PUT(value = "user/edit")
    Observable<ApiResponse<User>> editRemoteUser(@Header(Const.AUTHORIZATION) String token,
                                                 @Header(Const.AUTHORIZER) String authorizer,
                                                 @Body User updateUser);

    @PUT(value = "user/edit/display-name")
    Observable<ApiResponse<String>> editUserDisplayName(@Header(Const.AUTHORIZATION) String token,
                                                        @Header(Const.AUTHORIZER) String authorizer,
                                                        @Query("name") String newName);

    @PUT(value = "user/edit/user-connect-name")
    Observable<ApiResponse<User>> editUserConnectName(@Header(Const.AUTHORIZATION) String token,
                                                      @Header(Const.AUTHORIZER) String authorizer,
                                                      @Body User updateUser);

    @GET(value = "user/check-user-connect-name")
    Observable<ApiResponse<Boolean>> checkUserConnectName(@Header(Const.AUTHORIZATION) String token,
                                                          @Header(Const.AUTHORIZER) String authorizer,
                                                          @Query("username") String username);
}
