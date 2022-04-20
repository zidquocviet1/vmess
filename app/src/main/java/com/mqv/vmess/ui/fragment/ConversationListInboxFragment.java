package com.mqv.vmess.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.vmess.activity.MainActivity;
import com.mqv.vmess.activity.SearchConversationActivity;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.databinding.FragmentConversationBinding;
import com.mqv.vmess.ui.adapter.ConversationListAdapter;
import com.mqv.vmess.ui.fragment.viewmodel.ConversationFragmentViewModel;
import com.mqv.vmess.util.AlertDialogUtil;
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.util.RingtoneUtil;

import java.util.ArrayList;
import java.util.List;

public class ConversationListInboxFragment extends ConversationListFragment<ConversationFragmentViewModel, FragmentConversationBinding> {
    private boolean isFirstLoadingConversation;

    private static final String MESSAGE_RINGTONE = "message_receive.mp3";

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentConversationBinding.inflate(inflater, container, false);
    }

    @NonNull
    @Override
    public Class<ConversationFragmentViewModel> getViewModelClass() {
        return ConversationFragmentViewModel.class;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        registerEvent();
    }

    @Override
    public void setupObserver() {
        mViewModel.getRefreshConversationResult().observe(this, result -> {
            if (result == null) return;

            mBinding.swipeMessages.setRefreshing(result.getStatus() == NetworkStatus.LOADING);
        });

        mViewModel.getRefreshFailureResult().observe(this, event -> {
            if (event == null) return;

            Integer error = event.getContentIfNotHandled();

            if (error != null) {
                mBinding.swipeMessages.setRefreshing(false);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        mViewModel.getConversationListObserver().observe(this, updatedList -> {
            if (updatedList == null) return;

            mConversations = updatedList;
            mAdapter.submitList(new ArrayList<>(mConversations), () -> {
                List<String> onlineUsers = mViewModel.getPresenceUserListValue();

                if (onlineUsers != null) {
                    bindPresenceConversation(onlineUsers);
                }
            });
            mBinding.textNoChat.setVisibility((updatedList.isEmpty() && !isFirstLoadingConversation) ? View.VISIBLE : View.GONE);
        });

        mViewModel.getConversationInserted().observe(this, event -> {
            if (event != null && event.getContentIfNotHandled() != null) {
                RingtoneUtil.open(requireContext(), MESSAGE_RINGTONE);
            }
        });

        mViewModel.getPresenceUserListObserver().observe(this, this::bindPresenceConversation);

        mViewModel.getOneTimeLoadingResult().observe(this, pair -> {
            Boolean isLoading = pair.first;
            Integer content = pair.second;

            if (isLoading) {
                AlertDialogUtil.startLoadingDialog(requireContext(), requireActivity().getLayoutInflater(), content);
            } else {
                AlertDialogUtil.finishLoadingDialog();
            }
        });

        mViewModel.getOneTimeErrorObserver().observe(this, event -> {
            if (event == null) return;

            Integer content = event.getContentIfNotHandled();

            if (content != null) {
                Toast.makeText(requireContext(), content, Toast.LENGTH_SHORT).show();
            }
        });

        mViewModel.getLoadingConversationResult().observe(this, resultEvent -> {
            if (!mAdapter.getCurrentList().isEmpty()) return;
            if (resultEvent == null) return;

            Result<Boolean> result = resultEvent.getContentIfNotHandled();

            if (result != null) {
                switch (result.getStatus()) {
                    case LOADING:
                        isFirstLoadingConversation = true;

                        mBinding.recyclerMessages.setVisibility(View.GONE);
                        mBinding.progressBarLoading.setVisibility(View.VISIBLE);
                        mBinding.textNoChat.setVisibility(View.GONE);
                        break;
                    case SUCCESS:
                    case ERROR:
                    case TERMINATE:
                        isFirstLoadingConversation = false;

                        mBinding.recyclerMessages.setVisibility(View.VISIBLE);
                        mBinding.progressBarLoading.setVisibility(View.GONE);
                        mBinding.textNoChat.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
    }

    @NonNull
    @Override
    public SwipeRefreshLayout getSwipeLayout() {
        return mBinding.swipeMessages;
    }

    @Override
    public void onRefresh() {
        MainActivity activity = (MainActivity) requireActivity();

        if (activity.getNetworkStatus()) {
            mViewModel.onRefresh();
        } else {
            Toast.makeText(requireContext(), "Fetching new conversation when you are offline.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.forceClearDispose();
    }

    @Override
    public void initializeRecyclerview() {
        mConversations = new ArrayList<>();
        mAdapter = new ConversationListAdapter(getContext());
        mAdapter.registerOnConversationClick(onConversationClick());

        mBinding.recyclerMessages.setAdapter(mAdapter);
        mBinding.recyclerMessages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mBinding.recyclerMessages.setItemAnimator(new DefaultItemAnimator());
        mBinding.recyclerMessages.setNestedScrollingEnabled(false);
        mBinding.recyclerMessages.setHasFixedSize(false);
    }

    @Override
    public void postToRecyclerview(@NonNull Runnable runnable) {
        mBinding.recyclerMessages.post(runnable);
    }

    @Override
    public void onDataSizeChanged(boolean isEmpty) {

    }

    @Override
    protected void onConversationOpenResult(@Nullable ActivityResult result) {
    }

    private void registerEvent() {
        mBinding.searchButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), SearchConversationActivity.class)));
    }
}