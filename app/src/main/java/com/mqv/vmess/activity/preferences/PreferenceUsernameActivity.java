package com.mqv.vmess.activity.preferences;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.ToolbarActivity;
import com.mqv.vmess.activity.viewmodel.UsernameViewModel;
import com.mqv.vmess.databinding.ActivityPreferenceUsernameBinding;
import com.mqv.vmess.util.LoadingDialog;
import com.mqv.vmess.util.NetworkStatus;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

@AndroidEntryPoint
public class PreferenceUsernameActivity extends ToolbarActivity<UsernameViewModel, ActivityPreferenceUsernameBinding> {
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

    }

    @Override
    public void setupObserver() {
        mViewModel.getUsername().observe(this, username -> {
            currentUsername = username;
            mBinding.editUserName.setText(username);
            mBinding.textPromptLength.setText(getString(R.string.prompt_bio_length, username.length(), MAX_USERNAME_LENGTH));
            mViewModel.observeQueryTextChanged(createQueryUsername());
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

        mViewModel.getUsernameStatus().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            mBinding.progressBarLoading.setVisibility(status == NetworkStatus.LOADING ? View.VISIBLE : View.GONE);
            makeButtonEnable(status == NetworkStatus.SUCCESS);

            switch (status) {
                case ERROR:
                    mBinding.editUserName.setError(getString(result.getError()));
                    mBinding.editUserName.requestFocus();
                    break;
                case SUCCESS:
                    mBinding.editUserName.setError(null);
                    break;
            }
        });
    }

    private Observable<String> createQueryUsername() {
        PublishSubject<String> subject = PublishSubject.create();

        mBinding.editUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                subject.onNext(s.toString());
                mBinding.textPromptLength.setText(getString(R.string.prompt_bio_length, s.length(), MAX_USERNAME_LENGTH));
            }
        });

        return subject.debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io());
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