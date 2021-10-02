package com.mqv.realtimechatapplication.data.repository.impl;

import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.repository.EditUserPhotoRepository;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.service.UserService;
import com.mqv.realtimechatapplication.util.Const;

import java.io.File;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class EditUserPhotoRepositoryImpl implements EditUserPhotoRepository {
    private final UserService service;
    private final HistoryLoggedInUserDao historyUserDao;
    private final UserDao userDao;

    @Inject
    public EditUserPhotoRepositoryImpl(UserService service,
                                       HistoryLoggedInUserDao historyUserDao,
                                       UserDao userDao) {
        this.service = service;
        this.historyUserDao = historyUserDao;
        this.userDao = userDao;
    }

    @Override
    public Observable<ApiResponse<String>> updateProfilePicture(String token, String authorizer, String filePath) {
        var typeBody = RequestBody.create("profile_picture", MediaType.parse(Const.MULTIPART_TYPE));
        var file = new File(filePath);
        var fileBody = RequestBody.create(file, MediaType.parse(Const.MULTIPART_TYPE));
        var part = MultipartBody.Part.createFormData("photo", file.getName(), fileBody);
        return service.updateProfilePicture(token, authorizer, typeBody, part);
    }

    @Override
    public Observable<ApiResponse<String>> updateCoverPhoto(String token, String authorizer, String filePath) {
        return null;
    }

    @Override
    public Completable updateHistoryUserPhotoUrl(String uid, String photoUrl) {
        return historyUserDao.updatePhotoUrl(uid, photoUrl);
    }

    @Override
    public Completable updateCurrentUserPhotoUrl(String uid, String photoUrl) {
        return userDao.updateUserPhotoUrl(uid, photoUrl);
    }
}