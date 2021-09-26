package com.mqv.realtimechatapplication.di;

import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.repository.EditUserPhotoRepository;
import com.mqv.realtimechatapplication.data.repository.EditUserPhotoRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.HistoryLoggedInUserRepository;
import com.mqv.realtimechatapplication.data.repository.HistoryLoggedInUserRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.repository.LoginRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepositoryImpl;
import com.mqv.realtimechatapplication.network.service.FriendRequestService;
import com.mqv.realtimechatapplication.network.service.UserService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {
    @Provides
    @Singleton
    public LoginRepository provideLoginRes(UserService service, UserDao userDao, HistoryLoggedInUserDao historyLoggedInUserDao) {
        return new LoginRepositoryImpl(service, userDao, historyLoggedInUserDao);
    }

    @Provides
    @Singleton
    public EditUserPhotoRepository provideEditUserPhotoRes(UserService service,
                                                           HistoryLoggedInUserDao historyUserDao,
                                                           UserDao userDao) {
        return new EditUserPhotoRepositoryImpl(service, historyUserDao, userDao);
    }

    @Provides
    @Singleton
    public UserRepository provideUserRepository(UserService service, UserDao userDao) {
        return new UserRepositoryImpl(service, userDao);
    }

    @Provides
    @Singleton
    public HistoryLoggedInUserRepository provideHistoryUserRepo(HistoryLoggedInUserDao historyLoggedInUserDao) {
        return new HistoryLoggedInUserRepositoryImpl(historyLoggedInUserDao);
    }

    @Singleton
    @Provides
    public FriendRequestRepository provideFriendRequestRepo(FriendRequestService service) {
        return new FriendRequestRepositoryImpl(service);
    }
}
