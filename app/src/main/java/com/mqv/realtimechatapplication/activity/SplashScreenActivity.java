package com.mqv.realtimechatapplication.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.AndroidViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ActivitySplashScreenBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashScreenActivity extends BaseActivity<AndroidViewModel, ActivitySplashScreenBinding> {

    @Override
    public void binding() {
        mBinding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            // TODO: check user login session

            reloadFirebaseUser();

            var intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }else {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finishAfterTransition();
    }

    @Override
    public void setupObserver() {

    }
}