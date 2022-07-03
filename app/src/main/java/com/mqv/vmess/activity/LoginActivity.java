package com.mqv.vmess.activity;

import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.EMAIL_ERROR;
import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.PASSWORD_ERROR;
import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.SUCCESS;

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
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.LoginViewModel;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.databinding.ActivityLoginBinding;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.util.AlertDialogUtil;
import com.mqv.vmess.util.NetworkStatus;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends BaseActivity<LoginViewModel, ActivityLoginBinding> implements View.OnClickListener {
    private static final String STATE_EMAIL = "email";
    private static final String STATE_PASSWORD = "password";
    private EditText edtEmail, edtPassword;
    public static final String EXTRA_ACTION = "action";
    public static final int EXTRA_ADD_ACCOUNT = -1;
    private int mAction;
    private boolean isPendingLogin;
    private boolean isLoginSuccess;
    private FirebaseUser shouldSignInAgainUser;
    private FirebaseUser currentLoginUser;

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

        mAction = getIntent().getIntExtra(EXTRA_ACTION, 0);

        edtEmail = Objects.requireNonNull(mBinding.textLayoutEmail.getEditText());
        edtPassword = Objects.requireNonNull(mBinding.textLayoutPassword.getEditText());

        setupEvent();

        if (inState != null) {
            edtEmail.setText(inState.getString(STATE_EMAIL));
            edtPassword.setText(inState.getString(STATE_PASSWORD));
        }

        if (mAction == EXTRA_ADD_ACCOUNT) {
            mBinding.imageLoginQrCode.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_EMAIL, edtEmail.getText().toString().trim());
        outState.putString(STATE_PASSWORD, edtPassword.getText().toString().trim());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isPendingLogin) {
            if (mAction == EXTRA_ADD_ACCOUNT) {
                shouldSignInAgainUser = mViewModel.getPreviousFirebaseUser();

                currentLoginUser = FirebaseAuth.getInstance().getCurrentUser();
                mViewModel.setLoginUserOnStop(currentLoginUser);
                FirebaseAuth.getInstance().signOut();

                mViewModel.signInAgainFirebaseUser(shouldSignInAgainUser);
            } else
                FirebaseAuth.getInstance().signOut();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isPendingLogin) {
            if (mAction == EXTRA_ADD_ACCOUNT) {
                mViewModel.signInAgainFirebaseUser(currentLoginUser);
            } else {
                mViewModel.signInAgainFirebaseUser(mViewModel.getCurrentLoginFirebaseUser());
            }
        } else {
            if (isLoginSuccess && mAction == EXTRA_ADD_ACCOUNT) {
                mViewModel.signInAgainFirebaseUser(currentLoginUser);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Check when the user press back button
        if (isPendingLogin && mAction == EXTRA_ADD_ACCOUNT) {
            mViewModel.signInAgainFirebaseUser(shouldSignInAgainUser);
        }
    }

    @Override
    public void setupObserver() {
        mViewModel.getLoginValidationResult().observe(this, result -> {
            if (result == null)
                return;

            Result<User> loginResult = mViewModel.getLoginResult().getValue();
            boolean      isLoading   = loginResult != null && loginResult.getStatus() == NetworkStatus.LOADING;

            mBinding.buttonLogin.setEnabled(result == SUCCESS && !isLoading);

            if (result == EMAIL_ERROR) {
                mBinding.textLayoutEmail.setError(getString(result.getMessage()));
            } else mBinding.textLayoutEmail.setErrorEnabled(false);

            if (result == PASSWORD_ERROR) {
                mBinding.textLayoutPassword.setError(getString(result.getMessage()));
            } else mBinding.textLayoutPassword.setErrorEnabled(false);
        });

        mViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) return;

            setLoadingUi(loginResult.getStatus() == NetworkStatus.LOADING);
            isPendingLogin = loginResult.getStatus() == NetworkStatus.LOADING;

            if (loginResult.getStatus() == NetworkStatus.SUCCESS) {
                isLoginSuccess = true;

                LoggedInUserManager.getInstance().setLoggedInUser(loginResult.getSuccess());

                var mainIntent = new Intent(this, MainActivity.class);
                /*
                 * In here, the customer login new account from ManageAccountActivity.
                 * So we need to clear all of the activities in back stack
                 * */
                if (mAction == EXTRA_ADD_ACCOUNT) {
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }
                startActivity(mainIntent);
                finish();
            } else if (loginResult.getStatus() == NetworkStatus.ERROR) {
                if (mAction != EXTRA_ADD_ACCOUNT)
                    FirebaseAuth.getInstance().signOut();

                Toast.makeText(this, loginResult.getError(), Toast.LENGTH_SHORT).show();
            }
        });

        mViewModel.getDemoLoginResult().observe(this, result -> {
            if (result == null) return;

            if (result.getStatus() == NetworkStatus.SUCCESS) {
                AlertDialogUtil.finishLoadingDialog();

                LoggedInUserManager.getInstance().setLoggedInUser(result.getSuccess());
                Intent mainIntent = new Intent(this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            } else if (result.getStatus() == NetworkStatus.LOADING) {
                AlertDialogUtil.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
            } else {
                AlertDialogUtil.finishLoadingDialog();

                Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoadingUi(boolean isLoading) {
        mBinding.loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mBinding.buttonLogin.setEnabled(!isLoading);
        mBinding.buttonCreateAccount.setEnabled(!isLoading);
        mBinding.imageLoginPhone.setEnabled(!isLoading);
        mBinding.imageLoginQrCode.setEnabled(!isLoading);
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
                var email = edtEmail.getText().toString().trim();
                var password = edtPassword.getText().toString().trim();

                if (mAction == EXTRA_ADD_ACCOUNT) {
                    mViewModel.switchAccountWithEmailAndPassword(email, password);
                } else {
                    mViewModel.loginWithEmailAndPassword(email, password);
                }
            }
            return false;
        });

        mBinding.buttonLogin.setOnClickListener(this);
        mBinding.buttonCreateAccount.setOnClickListener(this);
        mBinding.textForgotPassword.setOnClickListener(this);
        mBinding.imageLoginPhone.setOnClickListener(this);
        mBinding.imageLoginFacebook.setOnClickListener(this);
        mBinding.imageLoginGoogle.setOnClickListener(this);
        mBinding.imageLoginQrCode.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == mBinding.buttonLogin.getId()) {
            mBinding.loading.setVisibility(View.VISIBLE);

            var email = edtEmail.getText().toString().trim();
            var password = edtPassword.getText().toString().trim();

            if (mAction == EXTRA_ADD_ACCOUNT) {
                mViewModel.switchAccountWithEmailAndPassword(email, password);
            } else {
                mViewModel.loginWithEmailAndPassword(email, password);
            }
        } else if (id == mBinding.buttonCreateAccount.getId()) {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        } else if (id == mBinding.textForgotPassword.getId()) {

        } else if (id == mBinding.imageLoginPhone.getId()) {
            startActivity(new Intent(LoginActivity.this, GenerateOtpActivity.class));
        } else if (id == mBinding.imageLoginFacebook.getId()) {

        } else if (id == mBinding.imageLoginGoogle.getId()) {
        } else if (id == mBinding.imageLoginQrCode.getId()) {
            mViewModel.loginForDemoSection();
        }
    }
}