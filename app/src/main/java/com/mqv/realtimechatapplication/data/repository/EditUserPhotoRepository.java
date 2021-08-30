package com.mqv.realtimechatapplication.data.repository;

import com.mqv.realtimechatapplication.network.ApiResponse;

import io.reactivex.rxjava3.core.Observable;

public interface EditUserPhotoRepository {
    Observable<ApiResponse<String>> updateProfilePicture(String token, String authorizer, String filePath);

    Observable<ApiResponse<String>> updateCoverPhoto(String token, String authorizer, String filePath);
}
