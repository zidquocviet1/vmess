package com.mqv.vmess.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.button.MaterialButton;
import com.mqv.vmess.R;

public abstract class ToolbarActivity<V extends ViewModel, B extends ViewBinding> extends BaseActivity<V, B> {
    private Button mButtonSave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /*
     * Must call in implementation onCreate after setContentView
     * This class has a child is that BaseUserActivity class. If the child of BaseUserActivity not has a appbar in the layout.
     * So we don't need to call all of these methods in current class
     * */
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

    //Make the save button visible and handle onClick method
    public void enableSaveButton(View.OnClickListener callback) {
        mButtonSave = findViewById(R.id.button_save);
        mButtonSave.setVisibility(View.VISIBLE);
        mButtonSave.setOnClickListener(callback);
    }

    public void makeButtonEnable(boolean isEnable){
        mButtonSave.setEnabled(isEnable);
    }

    public Button getToolbarButton() {
        return mButtonSave;
    }
}
