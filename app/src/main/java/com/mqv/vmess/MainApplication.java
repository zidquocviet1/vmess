package com.mqv.vmess;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.Data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.gson.Gson;
import com.mqv.vmess.activity.br.AlarmSleepTimer;
import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.activity.preferences.DarkMode;
import com.mqv.vmess.data.MyDatabase;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.dependencies.AppDependencyProvider;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.webrtc.CallManager;
import com.mqv.vmess.work.PushMessageAcknowledgeWorkWrapper;
import com.mqv.vmess.work.SubmitPreKeyBundleWorkWrapper;
import com.mqv.vmess.work.WorkDependency;

import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

@HiltAndroidApp
public class MainApplication extends Application implements Configuration.Provider {
    @Inject AppPreferences    mPreferences;
    @Inject HiltWorkerFactory workerFactory;
    @Inject OkHttpClient      okHttpClient;
    @Inject Gson              gson;
    @Inject MyDatabase        database;
    @Inject Retrofit          retrofit;

    private Activity activeActivity;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();

        initializeAppDependencies();
        setAppTheme(mPreferences.getDarkModeTheme());
        setupActivityListener();
        setupObserveIncomingMessage();
        initializeAlarmSleepTimer();
        initializeAuthUserToken();
        clearAllNotification(this);
        startTimerForCheckingAuthToken();
        notifyAllIncomingMessage();
        initializeRingRtc();
        registerAccountStateChangeForSubmitPreKeyBundle();

//        Notify play services to send socket connection heartbeat
        sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
        sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
    }

    private void setAppTheme(DarkMode mode){
        switch(mode){
            case ON:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case OFF:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                                .setWorkerFactory(workerFactory)
                                .build();
    }

    private void setupActivityListener() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                activeActivity = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                activeActivity = null;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                Logging.show("onActivityDestroyed: " + activity.getLocalClassName());
            }
        });
    }

    public Activity getActiveActivity() {
        return activeActivity;
    }

    private void initializeAppDependencies() {
        AppDependencies.init(new AppDependencyProvider(this, database, retrofit, okHttpClient, gson));
    }

    private void setupObserveIncomingMessage() {
//        AppDependencies.getIncomingMessageObserver();
    }

    private void initializeAlarmSleepTimer() {
        new AlarmSleepTimer(this).setRepeatingAlarm();
    }

    private void initializeAuthUserToken() {
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> checkAuthUserToken(false));
    }

    private void checkAuthUserToken(boolean forceRefresh) {
        FirebaseUser user                 = FirebaseAuth.getInstance().getCurrentUser();
        Long         authTokenExpiresTime = mPreferences.getUserAuthTokenExpiresTime();

        if (user == null) {
            mPreferences.setUserAuthToken("");
            mPreferences.setUserAuthTokenExpiresTime(0L);
        } else if (forceRefresh || (authTokenExpiresTime < Instant.now().getEpochSecond())) {
            user.getIdToken(true).addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    GetTokenResult tokenResult = result.getResult();

                    if (tokenResult != null) {
                        String token       = tokenResult.getToken();
                        long   expiresTime = tokenResult.getExpirationTimestamp();

                        Logging.show("Token: " + token);

                        mPreferences.setUserAuthToken(token);
                        mPreferences.setUserAuthTokenExpiresTime(expiresTime);
                    }
                }
            });
        }
    }

    public static void clearAllNotification(Context context) {
        NotificationManagerCompat.from(context).cancelAll();
    }

    private Runnable getAuthTokenChecker() {
        return () -> checkAuthUserToken(true);
    }

    private void startTimerForCheckingAuthToken() {
        mHandler.postDelayed(getAuthTokenChecker(), 55 * 60 * 1000);
    }

    private void stopTimerForCheckingAuthToken() {
        mHandler.removeCallbacks(getAuthTokenChecker());
    }

    private void notifyAllIncomingMessage() {
        //noinspection ResultOfMethodCallIgnored
        database.getChatDao()
                .fetchNotReceivedChatList()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flattenAsObservable(list -> list)
                .flatMap(c -> database.getChatDao()
                                      .update(c)
                                      .toSingleDefault(c.getId())
                                      .toObservable())
                .toList()
                .onErrorComplete()
                .subscribe(list -> {
                    if (!list.isEmpty()) {
                        Data data = new Data.Builder()
                                            .putStringArray(PushMessageAcknowledgeWorkWrapper.EXTRA_LIST_MESSAGE_ID, list.toArray(new String[0]))
                                            .putInt(PushMessageAcknowledgeWorkWrapper.EXTRA_TYPE, PushMessageAcknowledgeWorkWrapper.EXTRA_RECEIVED)
                                            .build();
                        WorkDependency.enqueue(new PushMessageAcknowledgeWorkWrapper(this, data));
                    }
                });
    }

    private void initializeRingRtc() {
        CallManager.initialize(this);
    }

    private void registerAccountStateChangeForSubmitPreKeyBundle() {
        AppDependencies.getDatabaseObserver().registerUserAccountListener(() -> SubmitPreKeyBundleWorkWrapper.enqueueIfNeeded(this));
    }
}
