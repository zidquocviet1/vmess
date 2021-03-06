package com.mqv.vmess.di;

import com.mqv.vmess.data.dao.ChatDao;
import com.mqv.vmess.data.dao.ConversationColorDao;
import com.mqv.vmess.data.dao.ConversationDao;
import com.mqv.vmess.data.dao.ConversationOptionDao;
import com.mqv.vmess.data.dao.FriendNotificationDao;
import com.mqv.vmess.data.dao.LinkMetadataDao;
import com.mqv.vmess.data.dao.LocalPlaintextContentDao;
import com.mqv.vmess.data.dao.PeopleDao;
import com.mqv.vmess.data.dao.RecentSearchDao;
import com.mqv.vmess.data.repository.ChatRepository;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.KeyRepository;
import com.mqv.vmess.data.repository.LinkMetadataRepository;
import com.mqv.vmess.data.repository.MediaRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.RtcRepository;
import com.mqv.vmess.data.repository.SearchRepository;
import com.mqv.vmess.data.repository.StorageRepository;
import com.mqv.vmess.data.repository.impl.ChatRepositoryImpl;
import com.mqv.vmess.data.repository.impl.ConversationRepositoryImpl;
import com.mqv.vmess.data.repository.impl.KeyRepositoryImpl;
import com.mqv.vmess.data.repository.impl.LinkMetadataRepositoryImpl;
import com.mqv.vmess.data.repository.impl.MediaRepositoryImpl;
import com.mqv.vmess.data.repository.impl.NotificationRepositoryImpl;
import com.mqv.vmess.data.repository.impl.PeopleRepositoryImpl;
import com.mqv.vmess.data.repository.impl.RtcRepositoryImpl;
import com.mqv.vmess.data.repository.impl.SearchRepositoryImpl;
import com.mqv.vmess.data.repository.impl.StorageRepositoryImpl;
import com.mqv.vmess.network.service.ChatService;
import com.mqv.vmess.network.service.ConversationService;
import com.mqv.vmess.network.service.FriendRequestService;
import com.mqv.vmess.network.service.KeyService;
import com.mqv.vmess.network.service.NotificationService;
import com.mqv.vmess.network.service.RtcService;
import com.mqv.vmess.network.service.StorageService;
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
                                                                ConversationOptionDao optionDao,
                                                                ConversationColorDao colorDao,
                                                                ChatDao chatDao,
                                                                LocalPlaintextContentDao localPlaintextContentDao) {
        return new ConversationRepositoryImpl(service, dao, optionDao, colorDao, chatDao, localPlaintextContentDao);
    }

    @ViewModelScoped
    @Provides
    public ChatRepository provideChatRepository(ChatDao dao, ChatService service) {
        return new ChatRepositoryImpl(dao, service);
    }

    @ViewModelScoped
    @Provides
    public StorageRepository provideStorageRepository(StorageService service) {
        return new StorageRepositoryImpl(service);
    }

    @ViewModelScoped
    @Provides
    public MediaRepository provideMediaRepository() {
        return new MediaRepositoryImpl();
    }

    @ViewModelScoped
    @Provides
    public SearchRepository provideRecentSearchRepository(RecentSearchDao dao, PeopleDao peopleDao) {
        return new SearchRepositoryImpl(dao, peopleDao);
    }

    @ViewModelScoped
    @Provides
    public LinkMetadataRepository provideLinkMetadataRepository(LinkMetadataDao dao) {
        return new LinkMetadataRepositoryImpl(dao);
    }

    @ViewModelScoped
    @Provides
    public RtcRepository provideRtcRepository(RtcService service) {
        return new RtcRepositoryImpl(service);
    }

    @ViewModelScoped
    @Provides
    public KeyRepository provideKeyRepository(KeyService keyService) {
        return new KeyRepositoryImpl(keyService);
    }
}
