package com.mqv.realtimechatapplication.data.repository;

import com.mqv.realtimechatapplication.data.datasource.LoginDataSource;
import com.mqv.realtimechatapplication.data.model.LoggedInUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.util.NetworkStatus;
import com.mqv.realtimechatapplication.util.Resource;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepositoryImpl implements LoginRepository{
    private final LoginDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private LoggedInUser user = null;

    @Inject
    public LoginRepositoryImpl(LoginDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean isLoggedIn() {
        return user != null;
    }

    @Override
    public void logout() {
        user = null;
        dataSource.logout();
    }

    private void setLoggedInUser(LoggedInUser user) {
        this.user = user;
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    @Override
    public Resource<LoggedInUser> login(String username, String password) {
        // handle login
        var result = dataSource.login(username, password);
        if (result.getStatus() == NetworkStatus.SUCCESS) {
            setLoggedInUser(result.getData());
        }
        return result;
    }

    @Override
    public Observable<ApiResponse<String>> test(String token) {
        return dataSource.test(token);
    }
}