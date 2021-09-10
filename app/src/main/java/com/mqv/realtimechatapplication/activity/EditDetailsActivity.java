package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.EditDetailsViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityEditDetailsBinding;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditDetailsActivity extends ToolbarActivity<EditDetailsViewModel, ActivityEditDetailsBinding> {

    @Override
    public void binding() {
        mBinding = ActivityEditDetailsBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<EditDetailsViewModel> getViewModelClass() {
        return EditDetailsViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_edit_details);

        var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        var navController = Objects.requireNonNull(navHostFragment).getNavController();
        var toolbar = (Toolbar) findViewById(R.id.toolbar);
        NavigationUI.setupWithNavController(toolbar, navController);
    }

    @Override
    public void setupObserver() {
        // default implementation method
    }
}