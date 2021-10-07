package com.mqv.realtimechatapplication.ui.fragment;

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

import com.mqv.realtimechatapplication.databinding.FragmentConversationBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.ui.adapter.ConversationAdapter;
import com.mqv.realtimechatapplication.ui.adapter.RankUserConversationAdapter;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel;

import java.util.ArrayList;

public class ConversationFragment extends BaseSwipeFragment<ConversationFragmentViewModel, FragmentConversationBinding>{
    private ConversationAdapter adapter;
    private RankUserConversationAdapter rankUserConversationAdapter;

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
    }

    @Override
    public void setupObserver() {
        if (mViewModel != null){
            mViewModel.getConversationListSafe().observe(this, conversations -> {
                if (conversations != null && !conversations.isEmpty()){
                    adapter.submitList(conversations);

                    stopRefresh();
                }
            });

            mViewModel.getRemoteUserListSafe().observe(this, remoteUsers -> {
                if (remoteUsers != null && !remoteUsers.isEmpty()){
                    rankUserConversationAdapter.submitList(remoteUsers);

                    stopRefresh();
                }
            });
        }
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

    private void setupRecyclerView(){
        var listConversation = new ArrayList<Conversation>();

        adapter = new ConversationAdapter(listConversation, getContext());
        adapter.submitList(listConversation);

        rankUserConversationAdapter = new RankUserConversationAdapter(getContext());

        mBinding.recyclerMessages.setAdapter(adapter);
        mBinding.recyclerMessages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mBinding.recyclerMessages.setItemAnimator(new DefaultItemAnimator());
        mBinding.recyclerMessages.setNestedScrollingEnabled(false);
        mBinding.recyclerMessages.setHasFixedSize(false);

        mBinding.recyclerRankChat.setAdapter(rankUserConversationAdapter);
        mBinding.recyclerRankChat.setLayoutManager(new GridLayoutManager(getContext(), 1, RecyclerView.HORIZONTAL, false));
        mBinding.recyclerRankChat.setItemAnimator(new DefaultItemAnimator());
    }
}
