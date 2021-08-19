package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

public abstract class BaseActivity<V extends ViewModel, B extends ViewBinding>
        extends AppCompatActivity {

    public B mBinding;
    public V mViewModel;

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
