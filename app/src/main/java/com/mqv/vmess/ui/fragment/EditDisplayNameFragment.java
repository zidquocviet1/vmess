package com.mqv.vmess.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.EditDetailsActivity;
import com.mqv.vmess.activity.viewmodel.EditDetailsViewModel;
import com.mqv.vmess.databinding.FragmentEditDisplayNameBinding;
import com.mqv.vmess.util.NetworkStatus;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditDisplayNameFragment extends BaseFragment<EditDetailsViewModel, FragmentEditDisplayNameBinding> {
    private NavController navController;
    private static final int MAX_NAME_LENGTH = 30;

    public EditDisplayNameFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentEditDisplayNameBinding.inflate(inflater, container, false);
    }

    @NonNull
    @Override
    public Class<EditDetailsViewModel> getViewModelClass() {
        return EditDetailsViewModel.class;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        var editDisplayName = Objects.requireNonNull(mBinding.textLayoutDisplayName.getEditText());
        var currentName = EditDisplayNameFragmentArgs.fromBundle(getArguments()).getDisplayName();
        editDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mBinding.includedButton.buttonBottom.setEnabled(s.length() != 0);
                mBinding.textLayoutDisplayName.setHelperText(getString(R.string.prompt_bio_length, s.length(), MAX_NAME_LENGTH));
                if (s.length() == 0) {
                    mBinding.textLayoutDisplayName.setError(getString(R.string.invalid_display_name));
                }
            }
        });
        editDisplayName.setText(currentName);

        mBinding.includedButton.buttonBottom.setOnClickListener(v -> {
            var newName = editDisplayName.getText().toString().trim();

            // Return to EditProfile screen when new name are the same with old name
            if (currentName.equals(newName)) {
                navigateToEditProfile();
                return;
            }

            mViewModel.updateUserDisplayName(newName);
        });
    }

    @Override
    public void setupObserver() {
        mViewModel.getUpdateResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            var status = result.getStatus();

            showLoadingUi(status == NetworkStatus.LOADING);

            if (status == NetworkStatus.SUCCESS) {
                var user = result.getSuccess();

                Toast.makeText(requireContext(), R.string.msg_update_user_info_successfully, Toast.LENGTH_SHORT).show();

                mViewModel.updateHistoryUserDisplayName(user.getUid(), user.getDisplayName());

                mViewModel.resetUpdateResult();

                ((EditDetailsActivity) requireActivity()).reloadFirebaseUser();

                navigateToEditProfile();
            } else if (status == NetworkStatus.ERROR) {
                Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToEditProfile() {
        var navOptions = new NavOptions.Builder().setPopUpTo(R.id.userEditDetailsFragment, true).build();
        navController.navigate(R.id.editDisplayNameFragment, null, navOptions);
        ((EditDetailsActivity) requireActivity()).onBackPressed();
    }

    private void showLoadingUi(boolean isLoading) {
        mBinding.loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mBinding.includedButton.buttonBottom.setEnabled(!isLoading);
    }
}