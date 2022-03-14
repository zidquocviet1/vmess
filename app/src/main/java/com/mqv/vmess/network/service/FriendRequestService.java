package com.mqv.vmess.network.service;

import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.FriendRequest;
import com.mqv.vmess.network.model.type.FriendRequestStatus;
import com.mqv.vmess.util.Const;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FriendRequestService {
    @GET(value = "connection/pending-connection")
    Observable<ApiResponse<List<FriendRequest>>> getAllPendingRequest(@Header(Const.AUTHORIZATION) String token);

    @GET(value = "connection/{receiverId}")
    Observable<ApiResponse<FriendRequestStatus>> findFriendRequestStatusByReceiverId(@Header(Const.AUTHORIZATION) String token,
                                                                                     @Path(value = "receiverId") String receiverId);

    @PUT(value = "connection/response-request")
    Observable<ApiResponse<Boolean>> responseFriendRequest(@Header(Const.AUTHORIZATION) String token,
                                                           @Header(Const.AUTHORIZER) String authorizer,
                                                           @Body FriendRequest request);

    @POST(value = "connection/connect")
    Observable<ApiResponse<Boolean>> requestConnect(@Header(Const.AUTHORIZATION) String token,
                                                    @Header(Const.AUTHORIZER) String authorizer,
                                                    @Body FriendRequest request);

    @GET(value = "connection/friend")
    Observable<ApiResponse<List<String>>> getFriendListId(@Header(Const.AUTHORIZATION) String token);

    @PUT(value = "connection/unfriend")
    Observable<ApiResponse<Boolean>> unfriend(@Header(Const.AUTHORIZATION) String token,
                                              @Query("uid") String uid);
}
