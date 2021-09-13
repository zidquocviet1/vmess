package com.mqv.realtimechatapplication.di;

import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.repository.EditUserPhotoRepository;
import com.mqv.realtimechatapplication.data.repository.EditUserPhotoRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.repository.LoginRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.RegisterRepository;
import com.mqv.realtimechatapplication.data.repository.RegisterRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepositoryImpl;
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
    public LoginRepository provideLoginRes(UserService service){
        return new LoginRepositoryImpl(service);
    }

    @Provides
    @Singleton
    public RegisterRepository provideRegisterRes(){
        return new RegisterRepositoryImpl();
    }

    @Provides
    @Singleton
    public EditUserPhotoRepository provideEditUserPhotoRes(UserService service){
        return new EditUserPhotoRepositoryImpl(service);
    }

    @Provides
    @Singleton
    public UserRepository provideUserRepository(UserService service, UserDao userDao){
        return new UserRepositoryImpl(service, userDao);
    }
}
