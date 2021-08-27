package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.button.MaterialButton;
import com.mqv.realtimechatapplication.R;

import dagger.hilt.android.AndroidEntryPoint;

public abstract class ToolbarActivity<V extends ViewModel, B extends ViewBinding> extends BaseActivity<V, B> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /*
     * Must call in implementation onCreate after setContentView*/
    public void setupToolbar() {
        var toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        var button = (MaterialButton) findViewById(R.id.button_back);
        button.setOnClickListener(v -> onBackPressed());
    }

    public void updateActionBarTitle(@StringRes int resId) {
        var actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(resId);
        }
    }
}
