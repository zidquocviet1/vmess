package com.mqv.realtimechatapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.MainViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityMainBinding;
import com.mqv.realtimechatapplication.util.Const;

import java.util.Objects;

public class MainActivity extends BaseActivity<MainViewModel, ActivityMainBinding>
        implements View.OnClickListener, NavController.OnDestinationChangedListener {
    private FirebaseUser user;

    @Override
    public void binding() {
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<MainViewModel> getViewModelClass() {
        return MainViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        var navController = Objects.requireNonNull(navHostFragment).getNavController();
        var bottomNavigationView = mBinding.bottomNav;
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        navController.addOnDestinationChangedListener(this);

        user = getIntent().getParcelableExtra(Const.EXTRA_USER_INFO);

        Glide.with(this)
                .load("https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__480.jpg")
                .error(R.drawable.ic_round_account)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .signature(new ObjectKey(user.getPhotoUrl() != null ? user.getPhotoUrl() : ""))
                .into(mBinding.imageAvatar);

        mBinding.imageAvatar.setOnClickListener(this);
    }

    @Override
    public void setupObserver() {
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == mBinding.imageAvatar.getId()) {
            var intent = new Intent(getApplicationContext(), UserActivity.class);
            intent.putExtra(Const.EXTRA_USER_INFO, user);
            startActivity(intent);
        }
    }

    @Override
    public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
        var id = navDestination.getId();

        if (id == R.id.peopleListFragment) {
            mBinding.textTitle.setText(R.string.title_people);
        } else if (id == R.id.conversationFragment) {
            mBinding.textTitle.setText(R.string.title_chat);
        }
    }
}