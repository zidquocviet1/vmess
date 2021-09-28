package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.ConnectPeopleViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityConnectPeopleBinding;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConnectPeopleActivity extends BaseActivity<ConnectPeopleViewModel, ActivityConnectPeopleBinding> {

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

        var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        var navController = Objects.requireNonNull(navHostFragment).getNavController();
        NavigationUI.setupWithNavController(mBinding.bottomNav, navController);
    }

    @Override
    public void setupObserver() {

    }
}