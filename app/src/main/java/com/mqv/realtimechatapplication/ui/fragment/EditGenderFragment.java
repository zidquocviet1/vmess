package com.mqv.realtimechatapplication.ui.fragment;

import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.EditDetailsActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.EditDetailsViewModel;
import com.mqv.realtimechatapplication.databinding.FragmentEditGenderBinding;
import com.mqv.realtimechatapplication.network.model.Gender;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditGenderFragment extends BaseFragment<EditDetailsViewModel, FragmentEditGenderBinding> {
    private final SparseArray<RadioButton> keyToRadio = new SparseArray<>();
    private final SparseIntArray radioToKey = new SparseIntArray();
    private Integer currentKey;
    private NavController navController;

    public EditGenderFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentEditGenderBinding.inflate(inflater, container, false);
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

        keyToRadio.put(Gender.MALE.getKey(), mBinding.radioMale);
        keyToRadio.put(Gender.FEMALE.getKey(), mBinding.radioFemale);
        keyToRadio.put(Gender.NON_BINARY.getKey(), mBinding.radioNonBinary);
        keyToRadio.put(Gender.TRANSGENDER.getKey(), mBinding.radioTransgender);
        keyToRadio.put(Gender.INTERSEX.getKey(), mBinding.radioIntersex);
        keyToRadio.put(Gender.PREFER_NOT_TO_SAY.getKey(), mBinding.radioNotToSay);

        radioToKey.put(mBinding.radioMale.getId(), Gender.MALE.getKey());
        radioToKey.put(mBinding.radioFemale.getId(), Gender.FEMALE.getKey());
        radioToKey.put(mBinding.radioNonBinary.getId(), Gender.NON_BINARY.getKey());
        radioToKey.put(mBinding.radioTransgender.getId(), Gender.TRANSGENDER.getKey());
        radioToKey.put(mBinding.radioIntersex.getId(), Gender.INTERSEX.getKey());
        radioToKey.put(mBinding.radioNotToSay.getId(), Gender.PREFER_NOT_TO_SAY.getKey());

        navController = Navigation.findNavController(view);

        mBinding.includedButton.buttonBottom.setOnClickListener(v -> {
            var checkedId = mBinding.radioGroupGender.getCheckedRadioButtonId();

            if (checkedId == -1){
                navigateToEditProfile();
                return;
            }

            var newGenderKey = radioToKey.get(checkedId);

            if (currentKey == newGenderKey) {
                navigateToEditProfile();
            } else {
                mViewModel.updateUserGender(Gender.getGenderByKey(newGenderKey));
            }
        });
    }

    @Override
    public void setupObserver() {
        mViewModel.getGenderAsKey().observe(getViewLifecycleOwner(), key -> {
            currentKey = key;
            if (key > 0)
                keyToRadio.get(key).setChecked(true);
        });

        mViewModel.getUpdateResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            var status = result.getStatus();

            mBinding.includedButton.buttonBottom.setEnabled(status != NetworkStatus.LOADING);
            mBinding.loading.setVisibility(status == NetworkStatus.LOADING ? View.VISIBLE : View.GONE);

            if (status == NetworkStatus.SUCCESS) {
                ((EditDetailsActivity) requireActivity()).updateLoggedInUser(result.getSuccess());

                navigateToEditProfile();

                mViewModel.resetUpdateResult();

                Toast.makeText(requireContext(), R.string.msg_update_user_info_successfully, Toast.LENGTH_SHORT).show();
            } else if (status == NetworkStatus.ERROR) {
                Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.resetUpdateResult();
    }

    private void navigateToEditProfile() {
        var navOptions = new NavOptions.Builder().setPopUpTo(R.id.userEditDetailsFragment, true).build();
        navController.navigate(R.id.editGenderFragment, null, navOptions);
        requireActivity().onBackPressed();
    }
}