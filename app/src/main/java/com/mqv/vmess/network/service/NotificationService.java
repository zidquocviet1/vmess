package com.mqv.vmess.network.service;

import com.mqv.vmess.data.model.FriendNotification;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.util.Const;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface NotificationService {
    @GET(value = "notification")
    Observable<ApiResponse<List<FriendNotification>>> fetchNotification(@Header(Const.AUTHORIZATION) String token,
                                                                        @Query("duration") int duration);

    @PUT(value = "notification/mark-as-read")
    Observable<ApiResponse<FriendNotification>> markAsRead(@Header(Const.AUTHORIZATION) String token,
                                                           @Body FriendNotification notification);

    @HTTP(method = "DELETE", path = "notification", hasBody = true)
    Observable<ApiResponse<FriendNotification>> removeNotification(@Header(Const.AUTHORIZATION) String token,
                                                                   @Body FriendNotification notification);

    @POST(value = "notification/find_by_uid")
    Observable<ApiResponse<FriendNotification>> findByUidAndAgentId(@Header(Const.AUTHORIZATION) String token,
                                                                    @Body FriendNotification notification);
}
