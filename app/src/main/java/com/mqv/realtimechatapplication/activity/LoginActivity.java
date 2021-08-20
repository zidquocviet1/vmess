package com.mqv.realtimechatapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.mqv.realtimechatapplication.activity.viewmodel.LoginViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityLoginBinding;
import com.mqv.realtimechatapplication.ui.data.LoggedInUserView;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends BaseActivity<LoginViewModel, ActivityLoginBinding> implements View.OnClickListener {
    private FirebaseAuth mAuth;

    @Override
    public void binding() {
        mBinding = ActivityLoginBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<LoginViewModel> getViewModelClass() {
        return LoginViewModel.class;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEvent();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void setupObserver() {
        mViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) {
                return;
            }
            mBinding.buttonLogin.setEnabled(loginFormState.isDataValid());

            if (loginFormState.getUsernameError() != null) {
                mBinding.textLayoutEmail.setError(getString(loginFormState.getUsernameError()));
            } else mBinding.textLayoutEmail.setErrorEnabled(false);

            if (loginFormState.getPasswordError() != null) {
                mBinding.textLayoutPassword.setError(getString(loginFormState.getPasswordError()));
            } else mBinding.textLayoutPassword.setErrorEnabled(false);
        });

        mViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) {
                return;
            }
            mBinding.loading.setVisibility(View.GONE);
            if (loginResult.getError() != null) {
                showLoginFailed(loginResult.getError());
            }
            if (loginResult.getSuccess() != null) {
                updateUiWithUser(loginResult.getSuccess());
            }
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        Toast.makeText(getApplicationContext(), "Login Successfully", Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private void setupEvent() {
        var edtEmail = Objects.requireNonNull(mBinding.textLayoutEmail.getEditText());
        var edtPassword = Objects.requireNonNull(mBinding.textLayoutPassword.getEditText());

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
                mViewModel.login(edtEmail.getText().toString(),
                        edtPassword.getText().toString());
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

            var edtEmail = Objects.requireNonNull(mBinding.textLayoutEmail.getEditText());
            var edtPassword = Objects.requireNonNull(mBinding.textLayoutPassword.getEditText());

            mViewModel.login(edtEmail.getText().toString(),
                    edtPassword.getText().toString());
        } else if (id == mBinding.buttonCreateAccount.getId()) {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        } else if (id == mBinding.textForgotPassword.getId()) {

        } else if (id == mBinding.imageLoginPhone.getId()) {
            startActivity(new Intent(LoginActivity.this, GenerateOtpActivity.class));
        } else if (id == mBinding.imageLoginFacebook.getId()) {

        } else if (id == mBinding.imageLoginGoogle.getId()) {
            var firebaseUser = mAuth.getCurrentUser();

            if (firebaseUser != null) {
                firebaseUser.getIdToken(true)
                        .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                            @Override
                            public void onComplete(@NonNull Task<GetTokenResult> task) {
                                if (task.isSuccessful()){
                                    var result = task.getResult();
                                    if (result != null){
                                        mViewModel.fetchCustomUserInfo("Bearer " + result.getToken());
                                    }
                                }
                            }
                        });
            }
        }
    }
}