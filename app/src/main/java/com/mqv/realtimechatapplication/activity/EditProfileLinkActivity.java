package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.EditProfileLinkViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityEditProfileLinkBinding;
import com.mqv.realtimechatapplication.network.model.UserSocialLink;
import com.mqv.realtimechatapplication.ui.adapter.UserLinkAdapter;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProfileLinkActivity extends ToolbarActivity<EditProfileLinkViewModel, ActivityEditProfileLinkBinding> implements View.OnClickListener {
    private UserLinkAdapter mAdapter;
    private EditText editAccountName;
    private List<UserSocialLink> mOldLinkList;

    @Override
    public void binding() {
        mBinding = ActivityEditProfileLinkBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<EditProfileLinkViewModel> getViewModelClass() {
        return EditProfileLinkViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_edit_links);

        setupRecyclerView();

        editAccountName = Objects.requireNonNull(mBinding.textLayoutAccountName.getEditText());
        editAccountName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                var imm = getSystemService(InputMethodManager.class);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                changeUiState(false);

                var accountName = v.getText().toString().trim();

                if (!TextUtils.isEmpty(accountName)) {
                    Logging.show(accountName);
                }
                return true;
            }
            return false;
        });
        mBinding.buttonSave.setOnClickListener(this);
        mBinding.buttonAddSocialLink.setOnClickListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getUserSocialLinkList().observe(this, links -> {
            mOldLinkList = links;
            mAdapter.submitList(links);
        });

        mViewModel.getUpdateResult().observe(this, result -> {

        });
    }

    private void setupRecyclerView() {
        mAdapter = new UserLinkAdapter(this, R.layout.item_individual_social_link, UserLinkAdapter.ACTION.EDIT);

        mBinding.recyclerEditLink.setAdapter(mAdapter);
        mBinding.recyclerEditLink.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.buttonSave.getId()) {
            var newLinkList = mAdapter.getCurrentList();

            var oldSize = mOldLinkList == null ? 0 : mOldLinkList.size();
            var newSize = newLinkList.size(); // don't need to check the null situation, because ListAdapter return NonNull

            Logging.show("New list size = " + newSize + " and old list size = " + oldSize);

            if (oldSize == 0 && newSize == 0)
                finish();

            if (oldSize != newSize) {
                mViewModel.updateUserSocialLink(newLinkList);
            } else {
                // Check whether the new Link list equals to old Link list or not
                var keyOldList = mOldLinkList.stream()
                        .map(UserSocialLink::getId)
                        .collect(Collectors.toList());

                var keyNewList = newLinkList.stream()
                        .map(link -> link.getId() == null ? -1 : link.getId())
                        .collect(Collectors.toList());

                // Compare to list {https://stackoverflow.com/questions/2762093/java-compare-two-lists}
                var similar = new HashSet<>(keyOldList);
                var different = new HashSet<>();

                different.addAll(keyOldList);
                different.addAll(keyNewList);

                similar.retainAll(keyNewList);
                different.removeAll(similar);

                if (different.size() == 0)
                    finish();
                else
                    mViewModel.updateUserSocialLink(newLinkList);
            }
        } else if (id == mBinding.buttonAddSocialLink.getId()) {
            changeUiState(true);
        }
    }

    private void changeUiState(boolean isSelectService) {
        mBinding.buttonAddSocialLink.setVisibility(isSelectService ? View.GONE : View.VISIBLE);
        mBinding.buttonSelectService.setVisibility(isSelectService ? View.VISIBLE : View.GONE);
        mBinding.textLayoutAccountName.setVisibility(isSelectService ? View.VISIBLE : View.GONE);
    }
}