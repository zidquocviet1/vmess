package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;

import androidx.lifecycle.AndroidViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ActivityAllPeopleBinding;
import com.mqv.realtimechatapplication.ui.adapter.PeopleAdapter;
import com.mqv.realtimechatapplication.ui.data.People;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AllPeopleActivity extends ToolbarActivity<AndroidViewModel, ActivityAllPeopleBinding> {
    private List<People> mPeopleList;

    @Override
    public void binding() {
        mBinding = ActivityAllPeopleBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_all_people);

        ArrayList<People> listPeople = getIntent().getParcelableArrayListExtra("list_people");

        if (listPeople == null) {
            mPeopleList = new ArrayList<>();
        } else {
            mPeopleList = new ArrayList<>(listPeople);
        }

        setupRecyclerView();
    }

    @Override
    public void setupObserver() {

    }

    private void setupRecyclerView() {
        var adapter = new PeopleAdapter(this);
        adapter.submitList(mPeopleList);

        mBinding.recyclerViewPeople.setAdapter(adapter);
        mBinding.recyclerViewPeople.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewPeople.setHasFixedSize(true);
    }
}