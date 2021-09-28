package com.mqv.realtimechatapplication.activity.preferences;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.FriendRequestViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceFriendRequestBinding;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;
import com.mqv.realtimechatapplication.ui.adapter.FriendRequestAdapter;
import com.mqv.realtimechatapplication.util.LoadingDialog;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceFriendRequestActivity extends ToolbarActivity<FriendRequestViewModel, ActivityPreferenceFriendRequestBinding> {
    private FriendRequestAdapter mAdapter;
    private final List<FriendRequest> mMutableList = new ArrayList<>();

    @Override
    public void binding() {
        mBinding = ActivityPreferenceFriendRequestBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<FriendRequestViewModel> getViewModelClass() {
        return FriendRequestViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_friend_request);

        setupRecyclerView();
    }

    @Override
    public void setupObserver() {
        mViewModel.getFriendRequestList().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            showLoadingUi(status == NetworkStatus.LOADING);

            switch (status) {
                case ERROR:
                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();

                    mBinding.recyclerViewRequest.setVisibility(View.GONE);
                    mBinding.imageError.setVisibility(View.VISIBLE);
                    mBinding.textError.setVisibility(View.VISIBLE);
                    mBinding.imageError.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.image_server_break_down));
                    mBinding.textError.setText(R.string.msg_oop_something_when_wrong);
                    break;
                case SUCCESS:
                    var list = result.getSuccess();
                    if (list == null || list.isEmpty()) {
                        mAdapter.submitList(new ArrayList<>());
                        mBinding.recyclerViewRequest.setVisibility(View.GONE);
                        mBinding.imageError.setVisibility(View.VISIBLE);
                        mBinding.textError.setVisibility(View.VISIBLE);
                        mBinding.imageError.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.image_no_data));
                        mBinding.textError.setText(R.string.msg_friend_request_empty_list);
                    } else {
                        mMutableList.addAll(list.stream().map(FriendRequest::new).collect(Collectors.toList()));
                        mAdapter.submitList(mMutableList);
                    }
                    break;
            }

        });
    }

    private void showLoadingUi(boolean isLoading) {
        mBinding.progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mBinding.recyclerViewRequest.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void setupRecyclerView() {
        mAdapter = new FriendRequestAdapter(this, mMutableList,
                confirmPosition -> handleAdapterClicked(confirmPosition, FriendRequestStatus.CONFIRM),
                cancelPosition -> handleAdapterClicked(cancelPosition, FriendRequestStatus.CANCEL));
        mAdapter.setOnRequestClicked(position -> handleAdapterClicked(position, FriendRequestStatus.PENDING));

        mBinding.recyclerViewRequest.setAdapter(mAdapter);
        mBinding.recyclerViewRequest.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewRequest.setHasFixedSize(true);
    }

    private void handleAdapterClicked(int position, FriendRequestStatus status) {
        switch (status) {
            case PENDING:
                break;
            case CANCEL:
                mAdapter.removeItem(position);
                break;
            case CONFIRM:
                LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.action_uploading_photo);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    mAdapter.removeItem(position);

                    Toast.makeText(getApplicationContext(), "Response request successfully", Toast.LENGTH_SHORT).show();

                    LoadingDialog.finishLoadingDialog();
                }, 2000);
                break;
        }
    }
}