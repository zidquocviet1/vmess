package com.mqv.realtimechatapplication.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.mqv.realtimechatapplication.util.MyActivityForResult;

public abstract class BaseActivity<V extends ViewModel, B extends ViewBinding>
        extends AppCompatActivity {

    public B mBinding;
    public V mViewModel;
    public MyActivityForResult<Intent, ActivityResult> activityResultLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.StartActivityForResult());

    public MyActivityForResult<String, Boolean> permissionLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.RequestPermission());
    public abstract void binding();

    @NonNull
    public abstract Class<V> getViewModelClass();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding();

        mViewModel = new ViewModelProvider(this).get(getViewModelClass());

        setContentView(mBinding.getRoot());

        setupObserver();
    }

    @Override
    protected void onDestroy() {
        if (mBinding != null) mBinding = null;
        super.onDestroy();
    }

    public abstract void setupObserver();
}
