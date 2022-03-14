package com.mqv.vmess.di;

import com.mqv.vmess.data.dao.HistoryLoggedInUserDao;
import com.mqv.vmess.data.dao.UserDao;
import com.mqv.vmess.data.repository.EditUserPhotoRepository;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.HistoryLoggedInUserRepository;
import com.mqv.vmess.data.repository.LoginRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.repository.impl.EditUserPhotoRepositoryImpl;
import com.mqv.vmess.data.repository.impl.FriendRequestRepositoryImpl;
import com.mqv.vmess.data.repository.impl.HistoryLoggedInUserRepositoryImpl;
import com.mqv.vmess.data.repository.impl.LoginRepositoryImpl;
import com.mqv.vmess.data.repository.impl.UserRepositoryImpl;
import com.mqv.vmess.network.service.FriendRequestService;
import com.mqv.vmess.network.service.UserService;

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
