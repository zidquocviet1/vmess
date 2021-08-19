package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.RegisterViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityRegisterBinding;
import com.mqv.realtimechatapplication.util.Const;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterActivity extends BaseActivity<RegisterViewModel, ActivityRegisterBinding> {
    private EditText usernameEdit, displayNameEdit, passwordEdit, rePasswordEdit;
    private boolean isPhoneType = false;

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
        mViewModel.getRegisterFormState().observe(this, state -> {
            if (state == null)
                return;

            mBinding.buttonRegister.setEnabled(state.isDataValid());

            if (state.getUsernameError() != null) {
                mBinding.textLayoutUsername.setError(getString(state.getUsernameError()));
            } else {
                mBinding.textLayoutUsername.setErrorEnabled(false);
            }

            if (state.getDisplayNameError() != null) {
                mBinding.textLayoutDisplayName.setError(getString(state.getDisplayNameError()));
            } else {
                mBinding.textLayoutDisplayName.setErrorEnabled(false);
            }

            if (state.getPasswordError() != null) {
                mBinding.textLayoutPassword.setError(getString(state.getPasswordError()));
            } else {
                mBinding.textLayoutPassword.setErrorEnabled(false);
            }

            if (state.getRePasswordError() != null) {
                mBinding.textLayoutRePassword.setError(getString(state.getRePasswordError()));
            } else {
                mBinding.textLayoutRePassword.setErrorEnabled(false);
            }
        });
    }

    private void setupEvent(){
        var textChanged = new TextWatcher(){
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
                        rePasswordEdit.getText().toString().trim(),
                        isPhoneType
                );
            }
        };

        usernameEdit.addTextChangedListener(textChanged);
        displayNameEdit.addTextChangedListener(textChanged);
        passwordEdit.addTextChangedListener(textChanged);
        rePasswordEdit.addTextChangedListener(textChanged);

        mBinding.textLayoutUsername.setEndIconOnClickListener(v -> {
            isPhoneType = !isPhoneType;
            if (!isPhoneType){
                usernameEdit.setHint(R.string.prompt_email);
                usernameEdit.setInputType(EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            }else{
                usernameEdit.setHint(R.string.prompt_phone_number);
                usernameEdit.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            }
            mBinding.textLayoutUsername.setEndIconActivated(isPhoneType);

            if (!TextUtils.isEmpty(usernameEdit.getText().toString().trim())){
                mViewModel.registerDataChanged(
                        usernameEdit.getText().toString().trim(),
                        displayNameEdit.getText().toString().trim(),
                        passwordEdit.getText().toString().trim(),
                        rePasswordEdit.getText().toString().trim(),
                        isPhoneType
                );
            }
        });

        mBinding.buttonRegister.setOnClickListener((v) -> {
            Map<String, Object> user = new HashMap<>();
            user.put(Const.KEY_USER_NAME, usernameEdit.getText().toString().trim());
            user.put(Const.KEY_DISPLAY_NAME, displayNameEdit.getText().toString().trim());
            user.put(Const.KEY_PASSWORD, passwordEdit.getText().toString().trim());
            mViewModel.register(user);
        });
    }
}