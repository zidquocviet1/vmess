package com.mqv.realtimechatapplication.di;

import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.data.dao.NotificationDao;
import com.mqv.realtimechatapplication.data.dao.PeopleDao;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.impl.ConversationRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.impl.NotificationRepositoryImpl;
import com.mqv.realtimechatapplication.data.repository.impl.PeopleRepositoryImpl;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.network.service.FriendRequestService;
import com.mqv.realtimechatapplication.network.service.NotificationService;
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
                                                    FriendRequestService friendRequestService) {
        return new PeopleRepositoryImpl(peopleDao, userService, friendRequestService);
    }

    @ViewModelScoped
    @Provides
    public NotificationRepository provideNotificationRepository(NotificationService service,
                                                                NotificationDao dao) {
        return new NotificationRepositoryImpl(service, dao);
    }

    @ViewModelScoped
    @Provides
    public ConversationRepository provideConversationRepository(ConversationService service,
                                                                ConversationDao dao) {
        return new ConversationRepositoryImpl(service, dao);
    }
}
