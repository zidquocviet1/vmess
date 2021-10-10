package com.mqv.realtimechatapplication.network.service;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.util.Const;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationService {
    @GET(value = "notification")
    Observable<ApiResponse<List<Notification>>> fetchNotification(@Header(Const.AUTHORIZATION) String token,
                                                                  @Header(Const.AUTHORIZER) String authorizer,
                                                                  @Query("uid") String uid,
                                                                  @Query("duration") int duration);

    @GET(value = "notification/unread/{uid}")
    Observable<ApiResponse<Integer>> getUnreadNotification(@Header(Const.AUTHORIZATION) String token,
                                                           @Header(Const.AUTHORIZER) String authorizer,
                                                           @Path("uid") String uid,
                                                           @Query("duration") int duration);

    @PUT(value = "notification/mark_as_read")
    Observable<ApiResponse<Notification>> markAsRead(@Header(Const.AUTHORIZATION) String token,
                                                     @Header(Const.AUTHORIZER) String authorizer,
                                                     @Body Notification notification);

    @HTTP(method = "DELETE", path = "notification", hasBody = true)
    Observable<ApiResponse<Notification>> removeNotification(@Header(Const.AUTHORIZATION) String token,
                                                             @Header(Const.AUTHORIZER) String authorizer,
                                                             @Body Notification notification);
}
