package com.mqv.vmess.data.repository;

import com.mqv.vmess.network.ApiResponse;

import java.io.File;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

public interface EditUserPhotoRepository {
    Observable<ApiResponse<String>> updateProfilePicture(String token, File file);

    Observable<ApiResponse<String>> updateCoverPhoto(String token, String filePath);

    Completable updateHistoryUserPhotoUrl(String uid, String photoUrl);

    Completable updateCurrentUserPhotoUrl(String uid, String photoUrl);
}
