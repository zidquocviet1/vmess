package com.mqv.vmess.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.vmess.activity.ConversationActivity;
import com.mqv.vmess.databinding.FragmentPeopleListBinding;
import com.mqv.vmess.ui.adapter.ActivePeopleAdapter;
import com.mqv.vmess.ui.adapter.BaseAdapter;
import com.mqv.vmess.ui.fragment.viewmodel.PeopleListFragmentViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PeopleListFragment extends BaseSwipeFragment<PeopleListFragmentViewModel, FragmentPeopleListBinding>
        implements BaseAdapter.ItemEventHandler {
    private ActivePeopleAdapter mAdapter;

    public PeopleListFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentPeopleListBinding.inflate(inflater, container, false);
    }

    @NonNull
    @Override
    public Class<PeopleListFragmentViewModel> getViewModelClass() {
        return PeopleListFragmentViewModel.class;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mAdapter = new ActivePeopleAdapter(requireContext());
        mAdapter.registerEventHandler(this);
        mBinding.recyclerActivePeople.setAdapter(mAdapter);
        mBinding.recyclerActivePeople.setHasFixedSize(true);
        mBinding.recyclerActivePeople.setLayoutManager(new LinearLayoutManager(requireContext()));

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setupObserver() {
        mViewModel.getActivePeopleListSafe().observe(getViewLifecycleOwner(), peopleList -> {
            mAdapter.submitList(peopleList);
        });
    }

    @NonNull
    @Override
    public SwipeRefreshLayout getSwipeLayout() {
        return mBinding.swipeMessages;
    }

    @Override
    public void onRefresh() {
        new Handler(Looper.getMainLooper()).postDelayed(this::stopRefresh, 2000);
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(requireContext(), ConversationActivity.class);
        intent.putExtra(ConversationActivity.EXTRA_PARTICIPANT_ID, mAdapter.getCurrentList().get(position).getUid());

        startActivity(intent);
    }

    @Override
    public void onItemLongClick(int position) {
    }

    @Override
    public void onListItemSizeChanged(int size) {
        mBinding.recyclerActivePeople.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        mBinding.textEmpty.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
        mBinding.imageNotActive.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
    }
}