package com.mqv.realtimechatapplication.di;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.network.adapter.LocalDateTimeAdapter;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.network.service.FriendRequestService;
import com.mqv.realtimechatapplication.network.service.NotificationService;
import com.mqv.realtimechatapplication.network.service.UserService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
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
        var loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        var client = new OkHttpClient.Builder()
                .connectTimeout(Const.NETWORK_TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(Const.NETWORK_TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(Const.NETWORK_TIME_OUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    var request = chain.request();
                    var requestBuilder = request.newBuilder()
                            .addHeader("Accept", Const.CONTENT_TYPE)
                            .addHeader("Content-Type", Const.CONTENT_TYPE);
                    return chain.proceed(requestBuilder.build());
                });
        // add self-signed SSL
        try {
            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            var fis = context.getResources().openRawResource(R.raw.tac);
            var bis = new BufferedInputStream(fis);
            var certificateFactory = CertificateFactory.getInstance("X.509");
            while (bis.available() > 0) {
                var cert = certificateFactory.generateCertificate(bis);
                keyStore.setCertificateEntry(Const.BASE_IP, cert);
            }

            var trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            var trustManagers = trustManagerFactory.getTrustManagers();
            if (!(trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager))) {
                Logging.show("Unexpected default trust manager " + Arrays.toString(trustManagers));
            }

            var x509TrustManager = (X509TrustManager) trustManagers[0];
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{x509TrustManager}, null);
            var sslSocketFactory = sslContext.getSocketFactory();

            client.sslSocketFactory(sslSocketFactory, x509TrustManager)
                    .hostnameVerifier((hostname, session) -> hostname.equals(Const.BASE_IP));
        } catch (KeyStoreException e) {
            e.printStackTrace();
            Logging.show("KeyStore exception");
        } catch (CertificateException e) {
            Logging.show("CertificateException exception");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            Logging.show("NoSuchAlgorithmException exception");
            e.printStackTrace();
        } catch (IOException e) {
            Logging.show("IO exception");
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return client.build();
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
}
