package com.mqv.vmess.di;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mqv.vmess.network.OkHttpProvider;
import com.mqv.vmess.network.adapter.LocalDateTimeAdapter;
import com.mqv.vmess.network.service.ChatService;
import com.mqv.vmess.network.service.ConversationService;
import com.mqv.vmess.network.service.FriendRequestService;
import com.mqv.vmess.network.service.NotificationService;
import com.mqv.vmess.network.service.RtcService;
import com.mqv.vmess.network.service.StorageService;
import com.mqv.vmess.network.service.UserService;
import com.mqv.vmess.util.Const;

import java.time.LocalDateTime;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@InstallIn(SingletonComponent.class)
@Module
public class NetworkModule {
    @Singleton
    @Provides
    public Gson provideGson() {
        var builder = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        return builder.create();
    }

    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClient(@ApplicationContext Context context) {
        return OkHttpProvider.provideSelfSignedCABuilder(context).build();
    }

    @Singleton
    @Provides
    @OkHttpProvider.UnsafeOkHttpClient
    public OkHttpClient providerUnsafeOkHttpClient() {
        return OkHttpProvider.provideAcceptAllCABuilder().build();
    }

    @Singleton
    @Provides
    public Retrofit provideRetrofit(Gson gson, OkHttpClient httpClient) {
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .baseUrl(Const.BASE_URL)
                .client(httpClient)
                .build();
    }

    @Singleton
    @Provides
    public UserService provideUserService(Retrofit retrofit) {
        return retrofit.create(UserService.class);
    }

    @Singleton
    @Provides
    public FriendRequestService provideFriendRequestService(Retrofit retrofit) {
        return retrofit.create(FriendRequestService.class);
    }

    @Provides
    @Singleton
    public NotificationService provideNotificationService(Retrofit retrofit) {
        return retrofit.create(NotificationService.class);
    }

    @Singleton
    @Provides
    public ConversationService provideConversationService(Retrofit retrofit) {
        return retrofit.create(ConversationService.class);
    }

    @Provides
    @Singleton
    public ChatService provideChatService(Retrofit retrofit) {
        return retrofit.create(ChatService.class);
    }

    @Singleton
    @Provides
    public StorageService provideStorageService(Retrofit retrofit) {
        return retrofit.create(StorageService.class);
    }

    @Singleton
    @Provides
    public RtcService provideRtcService(Retrofit retrofit) {
        return retrofit.create(RtcService.class);
    }
}
