package com.mqv.vmess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.AllPeopleViewModel;
import com.mqv.vmess.databinding.ActivityAllPeopleBinding;
import com.mqv.vmess.databinding.DialogPeopleDetailBinding;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.ui.adapter.PeopleAdapter;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.Picture;

import java.util.ArrayList;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AllPeopleActivity extends ToolbarActivity<AllPeopleViewModel, ActivityAllPeopleBinding> {
    private PeopleAdapter mAdapter;
    private AlertDialog peopleDialog;
    private DialogPeopleDetailBinding dialogPeopleDetailBinding;
    private User mCurrentUser;

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

        Objects.requireNonNull(mCurrentUser = LoggedInUserManager.getInstance().getLoggedInUser());
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
                        Toast.makeText(this, R.string.msg_unfriend_successfully, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        });

        mViewModel.getPeopleListObserver().observe(this, people -> {
            if (people != null) {
                mAdapter.submitList(new ArrayList<>(people));
            }
        });
    }

    private void setupRecyclerView() {
        mAdapter = new PeopleAdapter(this, this::onErrorButtonClick);

        mBinding.recyclerViewPeople.setAdapter(mAdapter);
        mBinding.recyclerViewPeople.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewPeople.setHasFixedSize(true);
    }

    private void onErrorButtonClick(Integer position, boolean isButtonClick) {
        if (isButtonClick) {
            showDialog(mAdapter.getCurrentList().get(position));
        } else {
            sendMessage(mAdapter.getCurrentList().get(position).getUid());
        }
    }

    private void showDialog(People item) {
        var view = getLayoutInflater().inflate(R.layout.dialog_people_detail, null);

        dialogPeopleDetailBinding = DialogPeopleDetailBinding.bind(view);

        peopleDialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        Picture.loadUserAvatarWithPlaceHolder(this, item.getPhotoUrl()).into(dialogPeopleDetailBinding.imageAvatar);

        dialogPeopleDetailBinding.textDisplayName.setText(item.getDisplayName());
        dialogPeopleDetailBinding.testUsername.setText(item.getUsername());
        dialogPeopleDetailBinding.buttonSendMessage.setOnClickListener(v -> sendMessage(item.getUid()));
        dialogPeopleDetailBinding.buttonUnfriend.setOnClickListener(v -> showUnfriendDialog(item));

        peopleDialog.show();
    }

    private void showUnfriendDialog(People item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.label_unfriend_question))
                .setMessage(getString(R.string.msg_unfriend_warning, item.getDisplayName()))
                .setPositiveButton(R.string.action_yes, (dialog, which) -> {
                    mViewModel.unfriend(mCurrentUser.getUid(), item);

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .create().show();
    }

    private void sendMessage(String participantId) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.EXTRA_PARTICIPANT_ID, participantId);

        startActivity(intent);
    }
}