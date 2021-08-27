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
import com.mqv.realtimechatapplication.util.MyActivityForResult;

import javax.inject.Inject;

public abstract class BaseActivity<V extends ViewModel, B extends ViewBinding>
        extends AppCompatActivity {

    public B mBinding;
    public V mViewModel;
    @Inject
    AppPreferences mPreferences;
    private boolean themeChangePending;
    private boolean paused;

    private final AppPreferences.Listener onPreferenceChanged = new AppPreferences.Listener() {
        @Override
        public void onDarkThemeModeChanged(DarkMode mode) {
            onThemeSettingsModeChange();
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
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mPreferences.addListener(onPreferenceChanged);
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
    }

    public abstract void setupObserver();

    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    private void onThemeSettingsModeChange() {
        if (paused) {
            themeChangePending = true;
        } else {
            recreate();
        }
    }
}
