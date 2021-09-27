package com.mqv.realtimechatapplication.network.service;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.util.Const;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface FriendRequestService {
    @GET(value = "connection/pending-connection")
    Observable<ApiResponse<List<FriendRequest>>> getAllPendingRequest(@Header(Const.AUTHORIZATION) String token,
                                                                      @Header(Const.AUTHORIZER) String authorizer);
}
