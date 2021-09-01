package com.mqv.realtimechatapplication.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.activity.preferences.AppPreferences;
import com.mqv.realtimechatapplication.activity.preferences.DarkMode;
import com.mqv.realtimechatapplication.network.firebase.FirebaseUserManager;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.MyActivityForResult;

import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.inject.Inject;

public abstract class BaseActivity<V extends ViewModel, B extends ViewBinding>
        extends AppCompatActivity {

    public B mBinding;
    public V mViewModel;
    @Inject
    AppPreferences mPreferences;
    private boolean themeChangePending;
    private boolean paused;
    private Consumer<FirebaseUser> firebaseUserConsumer;
    private FirebaseUserManager firebaseUserManager;

    private final AppPreferences.Listener onPreferenceChanged = new AppPreferences.Listener() {
        @Override
        public void onDarkThemeModeChanged(DarkMode mode) {
            onThemeSettingsModeChange();
        }
    };

    private final FirebaseUserManager.Listener onFirebaseUserChanged = new FirebaseUserManager.Listener() {
        @Override
        public void onUserChanged() {
            if (firebaseUserConsumer != null){
                firebaseUserConsumer.accept(getCurrentUser());
            }
        }
    };

    public MyActivityForResult<Intent, ActivityResult> activityResultLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.StartActivityForResult());

    public MyActivityForResult<String, Boolean> permissionLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.RequestPermission());

    public abstract void binding();

    public abstract Class<V> getViewModelClass();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding();

        if (getViewModelClass() != null) {
            mViewModel = new ViewModelProvider(this).get(getViewModelClass());
        }

        setContentView(mBinding.getRoot());

        setupObserver();

        firebaseUserManager = FirebaseUserManager.getInstance();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mPreferences.addListener(onPreferenceChanged);
        firebaseUserManager.addListener(onFirebaseUserChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;

        if (themeChangePending) {
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBinding != null) mBinding = null;
        mPreferences.removeListener(onPreferenceChanged);
        firebaseUserManager.removeListener(onFirebaseUserChanged);
    }

    public abstract void setupObserver();

    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void reloadFirebaseUser() {
        var executorService = Executors.newSingleThreadExecutor();

        getCurrentUser().reload().addOnCompleteListener(executorService, task -> {
            if (task.isSuccessful()) {
                Logging.show("Reload firebase user successfully");
                firebaseUserManager.emitListener();
            }
        });
    }

    public void registerFirebaseUserChange(Consumer<FirebaseUser> callback) {
        this.firebaseUserConsumer = callback;
    }

    private void onThemeSettingsModeChange() {
        if (paused) {
            themeChangePending = true;
        } else {
            recreate();
        }
    }
}
