package com.mqv.realtimechatapplication.di;

import com.mqv.realtimechatapplication.data.datasource.LoginDataSource;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.repository.LoginRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.RegisterRepository;
import com.mqv.realtimechatapplication.data.repository.RegisterRepositoryImpl;

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
    public LoginRepository provideLoginRes(LoginDataSource dataSource){
        return new LoginRepositoryImpl(dataSource);
    }

    @Provides
    @Singleton
    public RegisterRepository provideRegisterRes(){
        return new RegisterRepositoryImpl();
    }
}
