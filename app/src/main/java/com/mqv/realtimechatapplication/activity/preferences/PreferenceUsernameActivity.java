package com.mqv.realtimechatapplication.activity.preferences;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.UsernameViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceUsernameBinding;
import com.mqv.realtimechatapplication.util.LoadingDialog;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceUsernameActivity extends ToolbarActivity<UsernameViewModel, ActivityPreferenceUsernameBinding>
        implements TextWatcher {
    private static final int MAX_USERNAME_LENGTH = 20;
    private String currentUsername;

    @Override
    public void binding() {
        mBinding = ActivityPreferenceUsernameBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<UsernameViewModel> getViewModelClass() {
        return UsernameViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_username);

        enableSaveButton(handleButtonSaveClicked());

        mBinding.editUserName.addTextChangedListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getUsername().observe(this, username -> {
            currentUsername = username;
            mBinding.editUserName.setText(username);
        });

        mViewModel.getUpdateResult().observe(this, result -> {
            if (result == null) return;

            showLoadingUi(result.getStatus() == NetworkStatus.LOADING);

            if (result.getStatus() == NetworkStatus.SUCCESS) {
                updateLoggedInUser(result.getSuccess());

                Toast.makeText(this, R.string.msg_update_user_info_successfully, Toast.LENGTH_SHORT).show();

                finish();
            } else if (result.getStatus() == NetworkStatus.ERROR) {
                Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        mBinding.textPromptLength.setText(getString(R.string.prompt_bio_length, s.length(), MAX_USERNAME_LENGTH));
    }

    private void showLoadingUi(boolean isLoading) {
        mBinding.includedAppbar.buttonSave.setEnabled(!isLoading);
        if (isLoading) {
            LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
        } else {
            LoadingDialog.finishLoadingDialog();
        }
    }

    private View.OnClickListener handleButtonSaveClicked() {
        return v -> {
            var newUsername = mBinding.editUserName.getText().toString().trim();

            if (newUsername.equals(currentUsername)) {
                finish();
            } else {
                mViewModel.editUsername(newUsername);
            }
        };
    }
}