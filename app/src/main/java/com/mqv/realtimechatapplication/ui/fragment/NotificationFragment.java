package com.mqv.realtimechatapplication.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.realtimechatapplication.activity.preferences.PreferenceFriendRequestActivity;
import com.mqv.realtimechatapplication.databinding.FragmentNotificationBinding;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.ui.adapter.NotificationAdapter;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.NotificationFragmentViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationFragment extends BaseSwipeFragment<NotificationFragmentViewModel, FragmentNotificationBinding> {
    private final List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter mAdapter;
    private int currentPosition;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentNotificationBinding.inflate(inflater, container, false);
    }

    @Override
    public Class<NotificationFragmentViewModel> getViewModelClass() {
        return NotificationFragmentViewModel.class;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        registerEvent();
    }

    @Override
    public void setupObserver() {
        mViewModel.getNotificationListSafe().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            switch (status) {
                case ERROR:
                    Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    retrieveNotificationList(result.getSuccess());
                    break;
            }
        });

        mViewModel.getRefreshResultSafe().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            switch (status) {
                case ERROR:
                    stopRefresh();

                    Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    stopRefresh();

                    retrieveNotificationList(result.getSuccess());
                    break;
            }
        });

        mViewModel.getUpdateNotificationSafe().observe(getViewLifecycleOwner(), newItem -> {
            if (newItem == null)
                return;

            var oldItem = mAdapter.getCurrentList().get(currentPosition);

            oldItem.setHasRead(newItem.getHasRead());

            mAdapter.notifyItemChanged(currentPosition);

            mViewModel.resetUpdateResult();
        });
    }

    @NonNull
    @Override
    public SwipeRefreshLayout getSwipeLayout() {
        return mBinding.swipeMessages;
    }

    @Override
    public void onRefresh() {
        mViewModel.onRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.forceClearDispose();
    }

    private void retrieveNotificationList(List<Notification> data) {
        emptyListUi(data == null || data.isEmpty());

        if (data != null) {
            var sorted = data.stream()
                    .sorted((o1, o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()))
                    .collect(Collectors.toList());

            mAdapter.submitList(sorted);
        }
    }

    private void emptyListUi(boolean isEmpty) {
        mBinding.textNoData.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.imageNoData.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.recyclerNotification.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupRecyclerView() {
        mAdapter = new NotificationAdapter(requireContext());
        mAdapter.submitList(notifications);

        mBinding.recyclerNotification.setAdapter(mAdapter);
        mBinding.recyclerNotification.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.recyclerNotification.setNestedScrollingEnabled(false);
    }

    private void registerEvent() {
        mBinding.textSeeAll.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PreferenceFriendRequestActivity.class)));

        mAdapter.setOnItemClick(position -> {
            var notification = mAdapter.getCurrentList().get(position);

            if (!notification.getHasRead()) {
                currentPosition = position;
                mViewModel.markAsRead(notification);
            }

            switch (notification.getType()) {
                case NEW_FRIEND_REQUEST:
                    startActivity(new Intent(requireContext(), PreferenceFriendRequestActivity.class));
                    break;
                case ACCEPTED_FRIEND_REQUEST:
                    break;
            }
        });

        mAdapter.setOnChangeItem(position -> {
            var notification = mAdapter.getCurrentList().get(position);

            //TODO: show modal bottom sheet
        });
    }
}