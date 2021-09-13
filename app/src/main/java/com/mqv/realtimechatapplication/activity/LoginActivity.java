package com.mqv.realtimechatapplication.activity;

import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.EMAIL_ERROR;
import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.PASSWORD_ERROR;
import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.SUCCESS;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.LoginViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityLoginBinding;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends BaseActivity<LoginViewModel, ActivityLoginBinding> implements View.OnClickListener {
    private FirebaseAuth mAuth;
    private static final String STATE_EMAIL = "email";
    private static final String STATE_PASSWORD = "password";
    private EditText edtEmail, edtPassword;

    @Override
    public void binding() {
        mBinding = ActivityLoginBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<LoginViewModel> getViewModelClass() {
        return LoginViewModel.class;
    }

    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        edtEmail = Objects.requireNonNull(mBinding.textLayoutEmail.getEditText());
        edtPassword = Objects.requireNonNull(mBinding.textLayoutPassword.getEditText());

        setupEvent();

        mAuth = FirebaseAuth.getInstance();

        if (inState != null){
            edtEmail.setText(inState.getString(STATE_EMAIL));
            edtPassword.setText(inState.getString(STATE_PASSWORD));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_EMAIL, edtEmail.getText().toString().trim());
        outState.putString(STATE_PASSWORD, edtPassword.getText().toString().trim());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setupObserver() {
        mViewModel.getLoginValidationResult().observe(this, result -> {
            if (result == null)
                return;

            mBinding.buttonLogin.setEnabled(result == SUCCESS);

            if (result == EMAIL_ERROR) {
                mBinding.textLayoutEmail.setError(getString(result.getMessage()));
            } else mBinding.textLayoutEmail.setErrorEnabled(false);

            if (result == PASSWORD_ERROR) {
                mBinding.textLayoutPassword.setError(getString(result.getMessage()));
            } else mBinding.textLayoutPassword.setErrorEnabled(false);
        });

        mViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) return;

            if (loginResult.getStatus() == NetworkStatus.LOADING) {
                setLoadingUi(true);
            } else if (loginResult.getStatus() == NetworkStatus.SUCCESS) {
                setLoadingUi(false);

                LoggedInUserManager.getInstance().setLoggedInUser(loginResult.getSuccess());

                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                setLoadingUi(false);

                Toast.makeText(this, loginResult.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoadingUi(boolean isLoading) {
        mBinding.loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mBinding.buttonLogin.setEnabled(!isLoading);
        mBinding.buttonCreateAccount.setEnabled(!isLoading);
        mBinding.imageLoginPhone.setEnabled(!isLoading);
        mBinding.imageLoginFacebook.setEnabled(!isLoading);
        mBinding.imageLoginGoogle.setEnabled(!isLoading);
        mBinding.textForgotPassword.setClickable(!isLoading);
    }

    private void setupEvent() {
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                mViewModel.loginDataChanged(edtEmail.getText().toString(),
                        edtPassword.getText().toString());
            }
        };

        edtEmail.addTextChangedListener(afterTextChangedListener);
        edtPassword.addTextChangedListener(afterTextChangedListener);
        edtPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mViewModel.loginWithEmailAndPassword(edtEmail.getText().toString().trim(),
                        edtPassword.getText().toString().trim());
            }
            return false;
        });

        mBinding.buttonLogin.setOnClickListener(this);
        mBinding.buttonCreateAccount.setOnClickListener(this);
        mBinding.textForgotPassword.setOnClickListener(this);
        mBinding.imageLoginPhone.setOnClickListener(this);
        mBinding.imageLoginFacebook.setOnClickListener(this);
        mBinding.imageLoginGoogle.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == mBinding.buttonLogin.getId()) {
            mBinding.loading.setVisibility(View.VISIBLE);

            mViewModel.loginWithEmailAndPassword(edtEmail.getText().toString().trim(),
                    edtPassword.getText().toString().trim());
        } else if (id == mBinding.buttonCreateAccount.getId()) {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        } else if (id == mBinding.textForgotPassword.getId()) {

        } else if (id == mBinding.imageLoginPhone.getId()) {
            startActivity(new Intent(LoginActivity.this, GenerateOtpActivity.class));
        } else if (id == mBinding.imageLoginFacebook.getId()) {

        } else if (id == mBinding.imageLoginGoogle.getId()) {
        }
    }
}