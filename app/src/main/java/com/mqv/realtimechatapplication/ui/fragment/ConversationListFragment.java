package com.mqv.realtimechatapplication.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.realtimechatapplication.activity.ConversationActivity;
import com.mqv.realtimechatapplication.activity.SearchConversationActivity;
import com.mqv.realtimechatapplication.databinding.FragmentConversationBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.ui.adapter.ConversationListAdapter;
import com.mqv.realtimechatapplication.ui.adapter.RankUserConversationAdapter;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationListFragment extends BaseSwipeFragment<ConversationFragmentViewModel, FragmentConversationBinding> {
    private ConversationListAdapter mConversationAdapter;
    private RankUserConversationAdapter mRankUserAdapter;
    private final List<Conversation> mConversationList = new ArrayList<>();

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

        setupRecyclerView();
        registerEvent();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void setupObserver() {
        mViewModel.getConversationListSafe().observe(this, conversations -> {
            if (conversations != null && !conversations.isEmpty()) {
                var preSize = mConversationList.size();

                mConversationList.clear();
                mConversationAdapter.notifyItemRangeRemoved(0, preSize);

                mConversationList.addAll(conversations);
                mConversationAdapter.notifyItemRangeInserted(0, conversations.size());

                stopRefresh();
            }
        });

        mViewModel.getRemoteUserListSafe().observe(this, remoteUsers -> {
            if (remoteUsers != null && !remoteUsers.isEmpty()) {
                mRankUserAdapter.submitList(remoteUsers);

                stopRefresh();
            }
        });

        mViewModel.getInboxConversation().observe(this, inbox -> {
            if (inbox != null) {
                if (inbox.isEmpty()) {
                    mConversationList.clear();
                    mConversationAdapter.notifyDataSetChanged();
                } else {
                    mConversationList.clear();
                    mConversationList.addAll(inbox.stream()
                                                  .sorted((o1, o2) -> o2.getLastChat()
                                                                        .getTimestamp()
                                                                        .compareTo(o1.getLastChat().getTimestamp()))
                                                  .collect(Collectors.toList()));
                    mConversationAdapter.notifyItemRangeChanged(0, inbox.size());
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
        mViewModel.onRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.forceClearDispose();
    }

    private void setupRecyclerView() {
        mConversationAdapter = new ConversationListAdapter(mConversationList, getContext());
        mConversationAdapter.submitList(mConversationList);

        mRankUserAdapter = new RankUserConversationAdapter(getContext());

        mBinding.recyclerMessages.setAdapter(mConversationAdapter);
        mBinding.recyclerMessages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mBinding.recyclerMessages.setItemAnimator(new DefaultItemAnimator());
        mBinding.recyclerMessages.setNestedScrollingEnabled(false);
        mBinding.recyclerMessages.setHasFixedSize(false);

        mBinding.recyclerRankChat.setAdapter(mRankUserAdapter);
        mBinding.recyclerRankChat.setLayoutManager(new GridLayoutManager(getContext(), 1, RecyclerView.HORIZONTAL, false));
        mBinding.recyclerRankChat.setItemAnimator(new DefaultItemAnimator());
    }

    private void registerEvent() {
        mBinding.searchButton.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SearchConversationActivity.class));
        });

        mConversationAdapter.registerOnConversationClick(position -> {
            Conversation conversation = mConversationList.get(position);

            Intent conversationIntent = new Intent(requireContext(), ConversationActivity.class);
            conversationIntent.putExtra("conversation", conversation);
            startActivity(conversationIntent);
        });
    }
}
