package com.mqv.vmess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.ConnectPeopleViewModel;
import com.mqv.vmess.databinding.ActivityConnectPeopleBinding;
import com.mqv.vmess.util.AlertDialogUtil;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConnectPeopleActivity extends BaseActivity<ConnectPeopleViewModel, ActivityConnectPeopleBinding> {
    public static final String ACTION_FIND_USER = "find_user_by_id";
    public static final String EXTRA_USER_ID = "id";

    @Override
    public void binding() {
        mBinding = ActivityConnectPeopleBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<ConnectPeopleViewModel> getViewModelClass() {
        return ConnectPeopleViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var intent = getIntent();
        var action = intent.getAction();

        if (action != null && action.equals(ACTION_FIND_USER)) {
            mViewModel.getConnectUserByUid(intent.getStringExtra(EXTRA_USER_ID));

            mBinding.navHostFragment.setVisibility(View.GONE);
        } else {
            mBinding.navHostFragment.setVisibility(View.VISIBLE);

            var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            var navController = Objects.requireNonNull(navHostFragment).getNavController();
            NavigationUI.setupWithNavController(mBinding.bottomNav, navController);
        }
    }

    @Override
    public void setupObserver() {
        mViewModel.getConnectUserIdResult().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    AlertDialogUtil.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
                    break;
                case SUCCESS:
                    AlertDialogUtil.finishLoadingDialog();

                    var user = result.getSuccess();
                    var firebaseUser = mViewModel.getFirebaseUser().getValue();

                    if (firebaseUser != null) {
                        if (user.getUid().equals(firebaseUser.getUid())) {
                            Toast.makeText(this, R.string.msg_request_yourself, Toast.LENGTH_SHORT).show();
                        } else {
                            var intent = new Intent(this, RequestPeopleActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                        }
                        this.finish();
                    }
                    this.finish();
                    break;
                case ERROR:
                    AlertDialogUtil.finishLoadingDialog();

                    Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();

                    this.finish();
                    break;
            }
        });
    }
}