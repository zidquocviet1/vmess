package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.AllPeopleViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityAllPeopleBinding;
import com.mqv.realtimechatapplication.databinding.DialogPeopleDetailBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.ui.adapter.PeopleAdapter;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.Const;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AllPeopleActivity extends ToolbarActivity<AllPeopleViewModel, ActivityAllPeopleBinding> {
    private List<People> mPeopleList;
    private PeopleAdapter mAdapter;
    private AlertDialog peopleDialog;
    private DialogPeopleDetailBinding dialogPeopleDetailBinding;
    private int mCurrentPosition;

    @Override
    public void binding() {
        mBinding = ActivityAllPeopleBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AllPeopleViewModel> getViewModelClass() {
        return AllPeopleViewModel.class;
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
        mViewModel.getUnfriendResult().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    if (peopleDialog != null && peopleDialog.isShowing()) {
                        dialogPeopleDetailBinding.layoutContent.setVisibility(View.GONE);
                        dialogPeopleDetailBinding.progressBarLoading.setVisibility(View.VISIBLE);
                    }
                    break;
                case ERROR:
                    if (peopleDialog != null && peopleDialog.isShowing()) {
                        peopleDialog.dismiss();

                        Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case SUCCESS:
                    if (peopleDialog != null && peopleDialog.isShowing()) {
                        peopleDialog.dismiss();

                        mAdapter.removeItem(mCurrentPosition);

                        Toast.makeText(this, "Unfriend successfully", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        });
    }

    private void setupRecyclerView() {
        mAdapter = new PeopleAdapter(this, mPeopleList, this::onErrorButtonClick);
        mAdapter.submitList(mPeopleList);

        mBinding.recyclerViewPeople.setAdapter(mAdapter);
        mBinding.recyclerViewPeople.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewPeople.setHasFixedSize(true);
    }

    private void onErrorButtonClick(Integer position, boolean isButtonClick) {
        mCurrentPosition = position;

        if (isButtonClick) {
            showDialog(mAdapter.getCurrentList().get(position));
        } else {
            sendMessage();
        }
    }

    private void showDialog(People item) {
        var view = getLayoutInflater().inflate(R.layout.dialog_people_detail, null);

        dialogPeopleDetailBinding = DialogPeopleDetailBinding.bind(view);

        peopleDialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        var url = item.getPhotoUrl() == null ? "" : item.getPhotoUrl().replace("localhost", Const.BASE_IP);

        GlideApp.with(this)
                .load(url)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .signature(new ObjectKey(url))
                .error(R.drawable.ic_round_account)
                .into(dialogPeopleDetailBinding.imageAvatar);

        dialogPeopleDetailBinding.textDisplayName.setText(item.getDisplayName());
        dialogPeopleDetailBinding.testUsername.setText(item.getUsername());
        dialogPeopleDetailBinding.buttonSendMessage.setOnClickListener(v -> sendMessage());
        dialogPeopleDetailBinding.buttonUnfriend.setOnClickListener(v -> showUnfriendDialog(item));

        peopleDialog.show();
    }

    private void showUnfriendDialog(People item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Unfriend?")
                .setMessage(String.format("Are you sure you want to unfriend with %s?", item.getDisplayName()))
                .setPositiveButton(R.string.action_yes, (dialog, which) -> {
                    mViewModel.unfriend(item);

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .create().show();
    }

    private void sendMessage() {
        //TODO: open conversation
    }
}