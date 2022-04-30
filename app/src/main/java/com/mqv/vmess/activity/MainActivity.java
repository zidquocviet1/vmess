package com.mqv.vmess.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.MainApplication;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.listener.OnNetworkChangedListener;
import com.mqv.vmess.activity.preferences.PreferenceNotificationActivity;
import com.mqv.vmess.activity.viewmodel.MainViewModel;
import com.mqv.vmess.databinding.ActivityMainBinding;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.NetworkConstraint;
import com.mqv.vmess.network.websocket.WebSocketConnectionState;
import com.mqv.vmess.ui.data.UserSelection;
import com.mqv.vmess.ui.fragment.BaseFragment;
import com.mqv.vmess.ui.fragment.ConversationListFragment;
import com.mqv.vmess.ui.fragment.ConversationListInboxFragment;
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.util.Picture;

import java.util.ArrayList;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends BaseActivity<MainViewModel, ActivityMainBinding>
        implements View.OnClickListener,
                   NavController.OnDestinationChangedListener,
                   OnNetworkChangedListener,
                   ConversationListInboxFragment.ConversationSizeListener {
    private static final int MAX_BADGE_NUMBER = 99;

    private NavHostFragment navHostFragment;

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
        AppDependencies.getIncomingMessageObserver();
        registerNetworkEventCallback(this);

        super.onCreate(savedInstanceState);

        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        var navController = Objects.requireNonNull(navHostFragment).getNavController();
        var bottomNavigationView = mBinding.bottomNav;
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        navController.addOnDestinationChangedListener(this);

        mBinding.imageAvatar.setOnClickListener(this);
        mBinding.buttonAddConversation.setOnClickListener(this);
        mBinding.buttonQrScanner.setOnClickListener(this);
        mBinding.buttonAllPeople.setOnClickListener(this);
        mBinding.buttonNotificationSettings.setOnClickListener(this);

        registerFirebaseUserChange(user -> showUserImage(user == null ? null : user.getPhotoUrl()));

        AppDependencies.getMessageSenderProcessor();

        reloadFirebaseUser();
        registerOnSizedConversationListChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        MainApplication.clearAllNotification(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getUserPhotoUrl().observe(this, this::showUserImage);

        mViewModel.getRemoteUserResultSafe().observe(this, userResult -> {
            if (userResult == null) return;

            if (userResult.getStatus() == NetworkStatus.SUCCESS) {
                LoggedInUserManager.getInstance().setLoggedInUser(userResult.getSuccess());
            } else if (userResult.getStatus() == NetworkStatus.ERROR) {
                LoggedInUserManager.getInstance().setLoggedInUser(null);
            }
        });

        mViewModel.getNotificationBadgeResult().observe(this, number -> {
            var badge = mBinding.bottomNav.getOrCreateBadge(R.id.notification);
            badge.setVisible(number > 0);
            badge.setNumber(number);
            badge.setMaxCharacterCount(MAX_BADGE_NUMBER);
        });

        mViewModel.getConversationBadgeResult().observe(this, number -> {
            var badge = mBinding.bottomNav.getOrCreateBadge(R.id.chat);
            badge.setVisible(number > 0);
            badge.setNumber(number);
            badge.setMaxCharacterCount(MAX_BADGE_NUMBER);
            badge.setBackgroundColor(getColor(R.color.active_background_badge));
        });

        mViewModel.getPeopleActiveBadgeResult().observe(this, number -> {
            var badge = mBinding.bottomNav.getOrCreateBadge(R.id.people);
            badge.setVisible(number > 0);
            badge.setNumber(number);
            badge.setMaxCharacterCount(MAX_BADGE_NUMBER);
            badge.setBackgroundColor(getColor(R.color.active_background_badge));
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == mBinding.imageAvatar.getId()) {
            startActivity(UserActivity.class);
        } else if (id == mBinding.buttonAddConversation.getId()) {
            activityResultLauncher.launch(new Intent(this, AddConversationActivity.class), result -> {
                if (result != null && result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        ArrayList<UserSelection> selections = data.getParcelableArrayListExtra(AddGroupConversationActivity.EXTRA_GROUP_PARTICIPANTS);
                        Fragment visibleFragment = getVisibleFragment();

                        if (visibleFragment instanceof ConversationListFragment) {
                            //noinspection unchecked
                            ((ConversationListFragment)visibleFragment).createGroup(selections);
                        }
                    }
                }
            });
        } else if (id == mBinding.buttonQrScanner.getId()) {
            startActivity(ConnectPeopleActivity.class);
        } else if (id == mBinding.buttonAllPeople.getId()) {
            startActivity(AllPeopleActivity.class);
        } else if (id == mBinding.buttonNotificationSettings.getId()) {
            startActivity(PreferenceNotificationActivity.class);
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
            mBinding.buttonNotificationSettings.setVisibility(View.GONE);
        } else if (id == R.id.conversationFragment) {
            mBinding.toolbar.setTitle(R.string.title_chat);
            mBinding.buttonAllPeople.setVisibility(View.GONE);
            mBinding.buttonAddConversation.setVisibility(View.VISIBLE);
            mBinding.buttonQrScanner.setVisibility(View.VISIBLE);
            mBinding.buttonNotificationSettings.setVisibility(View.GONE);
        } else if (id == R.id.notification) {
            mBinding.toolbar.setTitle(R.string.title_notification);
            mBinding.buttonAllPeople.setVisibility(View.GONE);
            mBinding.buttonAddConversation.setVisibility(View.GONE);
            mBinding.buttonQrScanner.setVisibility(View.GONE);
            mBinding.buttonNotificationSettings.setVisibility(View.VISIBLE);
        }
    }

    @UiThread
    private void showUserImage(Uri photoUri) {
        runOnUiThread(() -> Picture.loadUserAvatar(this, photoUri == null ? null : photoUri.toString()).into(mBinding.imageAvatar));
    }

    private void startActivity(Class<?> target) {
        startActivity(new Intent(getApplicationContext(), target));
    }

    @Override
    public void onAvailable() {
    }

    @Override
    public void onLost() {
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onConnectionStateChanged(WebSocketConnectionState state) {
        if (state == WebSocketConnectionState.CONNECTED) {
            mBinding.textSubtitle.setVisibility(View.GONE);
            mViewModel.onFirstLoad();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                showUserImage(user.getPhotoUrl());
            }


            Fragment fragment = getVisibleFragment();

            if (fragment instanceof BaseFragment) {
                ((BaseFragment) fragment).onConnectionStateChanged();
            }
        } else if (state.isFailure() || state == WebSocketConnectionState.DISCONNECTED) {
            mBinding.textSubtitle.setVisibility(NetworkConstraint.isMet(this) ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onSizeChanged(int size) {
        mViewModel.observeConversationUnreadBadge(size);
    }

    private void registerOnSizedConversationListChanged() {
        Fragment fragment = getVisibleFragment();

        if (fragment instanceof ConversationListInboxFragment) {
            ConversationListInboxFragment conversationFragment = (ConversationListInboxFragment) fragment;
            conversationFragment.registerConversationSizedListener(this);
        }
    }

    public Fragment getVisibleFragment() {
        return navHostFragment.getChildFragmentManager().getFragments().get(0);
    }
}