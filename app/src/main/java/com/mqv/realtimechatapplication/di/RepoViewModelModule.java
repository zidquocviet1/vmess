package com.mqv.realtimechatapplication.di;

import com.mqv.realtimechatapplication.data.dao.PeopleDao;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.impl.PeopleRepositoryImpl;
import com.mqv.realtimechatapplication.network.service.FriendRequestService;
import com.mqv.realtimechatapplication.network.service.UserService;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.scopes.ViewModelScoped;

@Module
@InstallIn(ViewModelComponent.class)
public class RepoViewModelModule {
    @ViewModelScoped
    @Provides
    public PeopleRepository providePeopleRepository(PeopleDao peopleDao, UserService userService,
                                                    FriendRequestService friendRequestService){
        return new PeopleRepositoryImpl(peopleDao, userService, friendRequestService);
    }
}
