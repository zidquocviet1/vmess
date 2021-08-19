package com.mqv.realtimechatapplication.di;

import com.mqv.realtimechatapplication.data.datasource.LoginDataSource;
import com.mqv.realtimechatapplication.network.service.UserService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DataSourceModule {

    @Provides
    @Singleton
    public LoginDataSource provideLoginDataSource(UserService userService){
        return new LoginDataSource(userService);
    }
}
