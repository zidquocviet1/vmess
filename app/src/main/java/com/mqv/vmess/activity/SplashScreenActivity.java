package com.mqv.vmess.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.SplashScreenViewModel;
import com.mqv.vmess.databinding.ActivitySplashScreenBinding;

import dagger.hilt.android.AndroidEntryPoint;

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
public class SplashScreenActivity extends BaseActivity<SplashScreenViewModel, ActivitySplashScreenBinding> {

    @Override
    public void binding() {
        mBinding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<SplashScreenViewModel> getViewModelClass() {
        return SplashScreenViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            mViewModel.getCacheConversation();
            // TODO: check user login session
        }else {
            startActivity(LoginActivity.class);

            finishAfterTransition();
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void setupObserver() {
        mViewModel.getLoadCacheObserver().observe(this, isComplete -> {
            if (isComplete) {
                startActivity(MainActivity.class);
            } else {
                startActivity(LoginActivity.class);
            }
            finishAfterTransition();
        });
    }

    private void startActivity(@SuppressWarnings("rawtypes") Class clazz) {
        startActivity(new Intent(getApplicationContext(), clazz));
    }
}