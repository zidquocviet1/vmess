package com.mqv.vmess.network.service;

import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.ui.data.PhoneContact;
import com.mqv.vmess.util.Const;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {
    @POST(value = "user/login")
    Observable<ApiResponse<User>> loginWithToken(@Header(Const.AUTHORIZATION) String token,
                                                 @Header(Const.AUTHORIZER) String authorizer);

    @POST(value = "user/register")
    Observable<ApiResponse<String>> registerEmailAndPassword(@Query("email") String email,
                                                             @Query("password") String password,
                                                             @Query("displayName") String displayName);


    @POST(value = "user/fcm_token")
    Observable<ApiResponse<Object>> sendFcmTokenToServer(@Header(Const.AUTHORIZATION) String token,
                                                         @Header(Const.AUTHORIZER) String authorizer,
                                                         @Query("fcm_token") String fcmToken);

    @POST(value = "user/logout")
    Observable<ApiResponse<Boolean>> logout(@Header(Const.AUTHORIZATION) String token,
                                            @Header(Const.AUTHORIZER) String authorizer,
                                            @Query("fcm_token") String fcmToken);

    @GET(value = "user/info")
    Observable<ApiResponse<User>> fetchUserFromRemote(@Header(Const.AUTHORIZATION) String token,
                                                      @Query("uid") String uid);

    @GET(value = "user/info")
    Observable<ApiResponse<User>> fetchCustomUserInfo(@Header(Const.AUTHORIZATION) String token,
                                                      @Header(Const.AUTHORIZER) String authorizer);

    @PUT(value = "user/upload-photo")
    @Multipart
    Observable<ApiResponse<String>> updateProfilePicture(@Header(Const.AUTHORIZATION) String token,
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

    @GET(value = "user/connect-by-qr-code")
    Observable<ApiResponse<User>> getConnectUserByQrCode(@Header(Const.AUTHORIZATION) String token,
                                                         @Header(Const.AUTHORIZER) String authorizer,
                                                         @Query("code") String code);

    @GET(value = "user/connect-by-username")
    Observable<ApiResponse<User>> getConnectUserByUsername(@Header(Const.AUTHORIZATION) String token,
                                                           @Header(Const.AUTHORIZER) String authorizer,
                                                           @Query("username") String username);

    @GET(value = "user/connect-by-uid")
    Observable<ApiResponse<User>> getConnectUserByUid(@Header(Const.AUTHORIZATION) String token,
                                                      @Header(Const.AUTHORIZER) String authorizer,
                                                      @Query("uid") String uid);

    @GET(value = "user/connect-by-uid")
    Observable<ApiResponse<People>> getConnectPeopleByUid(@Header(Const.AUTHORIZATION) String token,
                                                          @Header(Const.AUTHORIZER) String authorizer,
                                                          @Query("uid") String uid);

    @GET(value = "user/phone-contact/{phone-number}")
    Observable<ApiResponse<PhoneContact>> getPhoneContactInfo(@Header(Const.AUTHORIZATION) String token,
                                                              @Path("phone-number") String phoneNumber,
                                                              @Query("cc") String countryCode);

    @FormUrlEncoded
    @POST(value = "user/login/demo-user")
    Observable<Response<ApiResponse<User>>> loginForDemoSection(@Field("token") String token);
}
