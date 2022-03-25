package com.mqv.vmess.di;

import com.mqv.vmess.data.dao.ChatDao;
import com.mqv.vmess.data.dao.ConversationDao;
import com.mqv.vmess.data.dao.FriendNotificationDao;
import com.mqv.vmess.data.dao.PeopleDao;
import com.mqv.vmess.data.repository.ChatRepository;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.impl.ChatRepositoryImpl;
import com.mqv.vmess.data.repository.impl.ConversationRepositoryImpl;
import com.mqv.vmess.data.repository.impl.NotificationRepositoryImpl;
import com.mqv.vmess.data.repository.impl.PeopleRepositoryImpl;
import com.mqv.vmess.network.service.ChatService;
import com.mqv.vmess.network.service.ConversationService;
import com.mqv.vmess.network.service.FriendRequestService;
import com.mqv.vmess.network.service.NotificationService;
import com.mqv.vmess.network.service.UserService;

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
                                                                FriendNotificationDao friendNotificationDao) {
        return new NotificationRepositoryImpl(service, friendNotificationDao);
    }

    @ViewModelScoped
    @Provides
    public ConversationRepository provideConversationRepository(ConversationService service,
                                                                ConversationDao dao,
                                                                ChatDao chatDao) {
        return new ConversationRepositoryImpl(service, dao, chatDao);
    }

    @ViewModelScoped
    @Provides
    public ChatRepository provideChatRepository(ChatDao dao, ChatService service) {
        return new ChatRepositoryImpl(dao, service);
    }
}
