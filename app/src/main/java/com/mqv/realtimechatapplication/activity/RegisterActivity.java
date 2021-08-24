package com.mqv.realtimechatapplication.activity;

import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.DISPLAY_NAME_ERROR;
import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.EMAIL_ERROR;
import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.PASSWORD_ERROR;
import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.RE_PASSWORD_ERROR;
import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.SUCCESS;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.mqv.realtimechatapplication.activity.viewmodel.RegisterViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityRegisterBinding;
import com.mqv.realtimechatapplication.util.Const;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterActivity extends BaseActivity<RegisterViewModel, ActivityRegisterBinding> {
    private EditText usernameEdit, displayNameEdit, passwordEdit, rePasswordEdit;

    @Override
    public void binding() {
        mBinding = ActivityRegisterBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<RegisterViewModel> getViewModelClass() {
        return RegisterViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usernameEdit = Objects.requireNonNull(mBinding.textLayoutUsername.getEditText());
        displayNameEdit = Objects.requireNonNull(mBinding.textLayoutDisplayName.getEditText());
        passwordEdit = Objects.requireNonNull(mBinding.textLayoutPassword.getEditText());
        rePasswordEdit = Objects.requireNonNull(mBinding.textLayoutRePassword.getEditText());

        setupEvent();
    }

    @Override
    public void setupObserver() {
        mViewModel.getRegisterValidationResult().observe(this, result -> {
            if (result == null)
                return;

            mBinding.buttonRegister.setEnabled(result == SUCCESS);

            if (result == EMAIL_ERROR) {
                mBinding.textLayoutUsername.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutUsername.setErrorEnabled(false);
            }

            if (result == DISPLAY_NAME_ERROR) {
                mBinding.textLayoutDisplayName.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutDisplayName.setErrorEnabled(false);
            }

            if (result == PASSWORD_ERROR) {
                mBinding.textLayoutPassword.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutPassword.setErrorEnabled(false);
            }

            if (result == RE_PASSWORD_ERROR) {
                mBinding.textLayoutRePassword.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutRePassword.setErrorEnabled(false);
            }
        });
    }

    private void setupEvent() {
        var textChanged = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mViewModel.registerDataChanged(
                        usernameEdit.getText().toString().trim(),
                        displayNameEdit.getText().toString().trim(),
                        passwordEdit.getText().toString().trim(),
                        rePasswordEdit.getText().toString().trim()
                );
            }
        };

        usernameEdit.addTextChangedListener(textChanged);
        displayNameEdit.addTextChangedListener(textChanged);
        passwordEdit.addTextChangedListener(textChanged);
        rePasswordEdit.addTextChangedListener(textChanged);

        mBinding.buttonRegister.setOnClickListener((v) -> {
            Map<String, Object> user = new HashMap<>();
            user.put(Const.KEY_USER_NAME, usernameEdit.getText().toString().trim());
            user.put(Const.KEY_DISPLAY_NAME, displayNameEdit.getText().toString().trim());
            user.put(Const.KEY_PASSWORD, passwordEdit.getText().toString().trim());
            mViewModel.register(user);
        });
    }
}