package com.mqv.realtimechatapplication.activity.preferences;

import static com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus.CANCEL;
import static com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus.CONFIRM;
import static com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus.PENDING;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.RequestPeopleActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.FriendRequestViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceFriendRequestBinding;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;
import com.mqv.realtimechatapplication.ui.adapter.FriendRequestAdapter;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.LoadingDialog;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceFriendRequestActivity extends ToolbarActivity<FriendRequestViewModel, ActivityPreferenceFriendRequestBinding> {
    private FriendRequestAdapter mAdapter;
    private final List<FriendRequest> mMutableList = new ArrayList<>();
    private int currentItemPosition;

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

        mViewModel.getResponseRequestResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    LoadingDialog.finishLoadingDialog();

                    mAdapter.removeItem(currentItemPosition);
                    break;
                case ERROR:
                    LoadingDialog.finishLoadingDialog();

                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        mViewModel.getConnectUserResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    LoadingDialog.finishLoadingDialog();

                    var intent = new Intent(this, RequestPeopleActivity.class);
                    intent.putExtra("user", result.getSuccess());

                    activityResultLauncher.launch(intent, data -> {
                        if (data != null && data.getResultCode() == RESULT_OK) {
                            var item = mAdapter.getCurrentList().get(currentItemPosition);

                            confirmFriendRequest(item.getSenderId(), item.getPhotoUrl(), item.getDisplayName());

                            mAdapter.removeItem(currentItemPosition);
                        }
                    });
                    break;
                case ERROR:
                    LoadingDialog.finishLoadingDialog();

                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
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
                confirmPosition -> handleAdapterClicked(confirmPosition, CONFIRM),
                cancelPosition -> handleAdapterClicked(cancelPosition, CANCEL));
        mAdapter.setOnRequestClicked(position -> handleAdapterClicked(position, PENDING));

        mBinding.recyclerViewRequest.setAdapter(mAdapter);
        mBinding.recyclerViewRequest.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewRequest.setHasFixedSize(true);
    }

    private void handleAdapterClicked(int position, FriendRequestStatus status) {
        var request = mAdapter.getCurrentList().get(position);

        currentItemPosition = position;

        switch (status) {
            case PENDING:
                mViewModel.getConnectUserByUid(request.getSenderId());
                break;
            case CANCEL:
                mViewModel.responseFriendRequest(new FriendRequest(request.getReceiverId(), request.getSenderId(), CANCEL));
                break;
            case CONFIRM:
                confirmFriendRequest(request.getSenderId(), request.getPhotoUrl(), request.getDisplayName());

                mViewModel.responseFriendRequest(new FriendRequest(request.getReceiverId(), request.getSenderId(), CONFIRM));
                break;
        }
    }

    private void confirmFriendRequest(String uid, String photoUrl, String displayName) {
        var people = new People(uid, displayName, photoUrl, null, LocalDateTime.now().minusMinutes(11));

        mViewModel.confirmFriendRequest(people);
    }
}