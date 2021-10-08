package com.mqv.realtimechatapplication.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.FragmentPeopleListBinding;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.PeopleListFragmentViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PeopleListFragment extends BaseSwipeFragment<PeopleListFragmentViewModel, FragmentPeopleListBinding> {
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
        super.onViewCreated(view, savedInstanceState);

        mBinding.buttonActive.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_peopleListFragment_to_activePeopleFragment2);
        });
    }

    @Override
    public void setupObserver() {
        mViewModel.getActivePeopleListSafe().observe(getViewLifecycleOwner(), peopleList -> {

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
}