package com.mqv.vmess.data.repository;

import com.mqv.vmess.network.ApiResponse;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

public interface EditUserPhotoRepository {
    Observable<ApiResponse<String>> updateProfilePicture(String token, String authorizer, String filePath);

    Observable<ApiResponse<String>> updateCoverPhoto(String token, String authorizer, String filePath);

    Completable updateHistoryUserPhotoUrl(String uid, String photoUrl);

    Completable updateCurrentUserPhotoUrl(String uid, String photoUrl);
}
