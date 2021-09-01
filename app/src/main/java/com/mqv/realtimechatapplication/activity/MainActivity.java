package com.mqv.realtimechatapplication.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
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
import com.mqv.realtimechatapplication.util.Logging;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends BaseActivity<MainViewModel, ActivityMainBinding>
        implements View.OnClickListener, NavController.OnDestinationChangedListener {
    @Override
    public void binding() {
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<MainViewModel> getViewModelClass() {
        return MainViewModel.class;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logging.show("Receiver is triggered");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        var navController = Objects.requireNonNull(navHostFragment).getNavController();
        var bottomNavigationView = mBinding.bottomNav;
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        navController.addOnDestinationChangedListener(this);

        mBinding.imageAvatar.setOnClickListener(this);
        mBinding.buttonAddConversation.setOnClickListener(this);

        // TODO: using request network callback instead
        var intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, intentFilter);

        registerFirebaseUserChange(this::showUserUi);
    }

    @Override
    public void setupObserver() {
        mViewModel.getFirebaseUser().observe(this, this::showUserUi);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == mBinding.imageAvatar.getId()) {
            var intent = new Intent(getApplicationContext(), UserActivity.class);
            startActivity(intent);
        }else if (id == mBinding.buttonAddConversation.getId()){
            mBinding.textSubtitle.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestinationChanged(@NonNull NavController navController,
                                     @NonNull NavDestination navDestination,
                                     @Nullable Bundle bundle) {
        var id = navDestination.getId();

        if (id == R.id.peopleListFragment || id == R.id.activePeopleFragment2) {
            mBinding.toolbar.setTitle(R.string.title_people);
            mBinding.buttonAllPeople.setVisibility(View.VISIBLE);
            mBinding.buttonAddConversation.setVisibility(View.GONE);
            mBinding.buttonQrScanner.setVisibility(View.GONE);
        } else if (id == R.id.conversationFragment) {
            mBinding.toolbar.setTitle(R.string.title_chat);
            mBinding.buttonAllPeople.setVisibility(View.GONE);
            mBinding.buttonAddConversation.setVisibility(View.VISIBLE);
            mBinding.buttonQrScanner.setVisibility(View.VISIBLE);
        }
    }

    @UiThread
    private void showUserUi(FirebaseUser user){
        if (user == null) return;

        // TODO: reformat the photoURL in the dev mode
        if (user.getPhotoUrl() != null){
            var reformatPhotoUrl = user.getPhotoUrl().toString().replace("localhost", Const.BASE_IP);
            Glide.with(this)
                    .load(reformatPhotoUrl)
                    .error(R.drawable.ic_round_account)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .signature(new ObjectKey(reformatPhotoUrl))
                    .into(mBinding.imageAvatar);
        }
    }
}