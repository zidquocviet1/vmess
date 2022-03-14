package com.mqv.vmess.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.EditProfileLinkViewModel;
import com.mqv.vmess.databinding.ActivityEditProfileLinkBinding;
import com.mqv.vmess.network.model.type.SocialType;
import com.mqv.vmess.network.model.UserSocialLink;
import com.mqv.vmess.ui.adapter.UserLinkAdapter;
import com.mqv.vmess.ui.fragment.SocialLinkListDialogFragment;
import com.mqv.vmess.util.LoadingDialog;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.util.NetworkStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProfileLinkActivity extends ToolbarActivity<EditProfileLinkViewModel, ActivityEditProfileLinkBinding>
        implements View.OnClickListener, SocialLinkListDialogFragment.OnSocialPlatformSelected,
        UserLinkAdapter.OnEditSocialLinkItemListener {
    private UserLinkAdapter mAdapter;
    private SocialType mSelectedSocialType;
    private Button mSelectedButton;
    private List<UserSocialLink> mOldLinkList;
    private final List<UserSocialLink> mNewLinkList = new ArrayList<>();
    private ArrayList<SocialType> socialTypeArrayList;

    public static final String ARG_ACTIVITY_CALLER = "activity_caller";
    public static final String ARG_ADAPTER_CALLER = "adapter_caller";

    private OnSocialSelectedFromAdapterListener callback;

    public interface OnSocialSelectedFromAdapterListener {
        void onSocialSelected(Button button, SocialType type);
    }

    public void setOnSocialSelectedFromAdapterListener(OnSocialSelectedFromAdapterListener callback) {
        this.callback = callback;
    }

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

        enableSaveButton(this);

        setupRecyclerView();

        socialTypeArrayList = SocialType.getSocialTypeAsArray();

        var editAccountName = Objects.requireNonNull(mBinding.textLayoutAccountName.getEditText());
        editAccountName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                var imm = getSystemService(InputMethodManager.class);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                changeUiState(false);

                var accountName = v.getText().toString().trim();

                if (!TextUtils.isEmpty(accountName)) {
                    var newUserSocialLink = new UserSocialLink(System.currentTimeMillis(), mSelectedSocialType, accountName);

                    mAdapter.addItem(newUserSocialLink);
                }

                v.setText("");
                mSelectedSocialType = null;
                mBinding.buttonSelectService.setText(R.string.action_select_platform);
                mBinding.textLayoutAccountName.setEnabled(false);
                return true;
            }
            return false;
        });
        mBinding.buttonAddSocialLink.setOnClickListener(this);
        mBinding.buttonSelectService.setOnClickListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getUserSocialLinkList().observe(this, links -> {
            mOldLinkList = links == null ? new ArrayList<>() : links.stream()
                    .map(u -> new UserSocialLink(u.getId(), u.getType(), u.getAccountName()))
                    .collect(Collectors.toList());

            mNewLinkList.addAll(links == null ? new ArrayList<>() : links);
            mAdapter.submitList(mNewLinkList);
        });

        mViewModel.getUpdateResult().observe(this, result -> {
            if (result == null) return;

            makeButtonEnable(result.getStatus() != NetworkStatus.LOADING);

            if (result.getStatus() == NetworkStatus.LOADING) {
                LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
            } else if (result.getStatus() == NetworkStatus.SUCCESS) {
                LoadingDialog.finishLoadingDialog();

                updateLoggedInUser(result.getSuccess());

                Toast.makeText(this, R.string.msg_update_user_info_successfully, Toast.LENGTH_SHORT).show();

                finish();
            } else {
                LoadingDialog.finishLoadingDialog();

                Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        mAdapter = new UserLinkAdapter(this, mNewLinkList, R.layout.item_individual_social_link, UserLinkAdapter.ACTION.EDIT);
        mBinding.recyclerEditLink.setAdapter(mAdapter);
        mBinding.recyclerEditLink.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mBinding.recyclerEditLink.setHasFixedSize(false);
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.includedAppbar.buttonSave.getId()) {
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
                        .map(UserSocialLink::getId)
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
        } else if (id == mBinding.buttonSelectService.getId()) {
            showSocialLinkFragmentDialog(ARG_ACTIVITY_CALLER);
        }
    }

    private void changeUiState(boolean isSelectService) {
        mBinding.buttonAddSocialLink.setVisibility(isSelectService ? View.GONE : View.VISIBLE);
        mBinding.buttonSelectService.setVisibility(isSelectService ? View.VISIBLE : View.GONE);
        mBinding.textLayoutAccountName.setVisibility(isSelectService ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPlatformSelected(int position) {
        mSelectedSocialType = (position < 0 || position >= socialTypeArrayList.size()) ? null : socialTypeArrayList.get(position);
        var socialName = mSelectedSocialType != null ? mSelectedSocialType.getValue() : getString(R.string.action_select_platform);
        var capitalizeName = socialName.substring(0, 1).toUpperCase(Locale.ROOT) + socialName.substring(1);
        mBinding.buttonSelectService.setText(capitalizeName);
        mBinding.textLayoutAccountName.setEnabled(mSelectedSocialType != null);
    }

    @Override
    public void onPlatformSelectedFromAdapter(int position) {
        mSelectedSocialType = (position < 0 || position >= socialTypeArrayList.size())
                ? null : socialTypeArrayList.get(position);
        if (callback != null) {
            callback.onSocialSelected(mSelectedButton, mSelectedSocialType);
        }
    }

    @Override
    public void onOpenPlatformSelection(Button button, UserSocialLink item) {
        showSocialLinkFragmentDialog(ARG_ADAPTER_CALLER);
        mSelectedButton = button;
    }

    private void showSocialLinkFragmentDialog(String caller) {
        var socialTypeNameList = socialTypeArrayList.stream()
                .map(SocialType::getValue)
                .collect(Collectors.toCollection(ArrayList::new));

        var socialLinkListBottomSheetFragment =
                SocialLinkListDialogFragment.newInstance(socialTypeNameList, caller);
        socialLinkListBottomSheetFragment.show(getSupportFragmentManager(), "Social List");
    }
}