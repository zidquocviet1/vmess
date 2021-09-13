package com.mqv.realtimechatapplication.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.EditDetailsActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.EditDetailsViewModel;
import com.mqv.realtimechatapplication.databinding.FragmentEditBirthdayBinding;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public class EditBirthdayFragment extends BaseFragment<EditDetailsViewModel, FragmentEditBirthdayBinding> {
    private EditText editBirthday;
    private LocalDateTime currentDateOfBirth;
    private NavController navController;

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentEditBirthdayBinding.inflate(inflater, container, false);
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

        editBirthday = Objects.requireNonNull(mBinding.textLayoutBirthday.getEditText());

        mBinding.datePicker.setOnDateChangedListener((view1, year, monthOfYear, dayOfMonth) -> {
            var selectedDateTime = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0);
            var stringDateOfBirth = selectedDateTime.format(UserEditDetailsFragment.NORMAL_FORMATTER);
            editBirthday.setText(stringDateOfBirth);
        });

        mBinding.includedButton.buttonBottom.setOnClickListener(v -> {
            var dayOfMonth = mBinding.datePicker.getDayOfMonth();
            var month = mBinding.datePicker.getMonth() + 1;
            var year = mBinding.datePicker.getYear();

            if (currentDateOfBirth == null) {
                mViewModel.updateUserBirthday(LocalDateTime.of(year, month, dayOfMonth, 0, 0));
            } else {
                var currentDayOfMonth = currentDateOfBirth.getDayOfMonth();
                var currentMonth = currentDateOfBirth.getMonth().getValue();
                var currentYear = currentDateOfBirth.getYear();

                if (currentYear == year && currentMonth == month && currentDayOfMonth == dayOfMonth) {
                    navigateToEditProfile();
                } else {
                    mViewModel.updateUserBirthday(LocalDateTime.of(year, month, dayOfMonth, 0, 0));
                }
            }
        });
    }

    @Override
    public void setupObserver() {
        mViewModel.getBirthday().observe(getViewLifecycleOwner(), birthday -> {
            int dayOfMonth, month, year;

            currentDateOfBirth = birthday;
            mBinding.text2.setVisibility(birthday == null ? View.VISIBLE : View.GONE);

            if (birthday != null) {
                var stringDateOfBirth = birthday.format(UserEditDetailsFragment.NORMAL_FORMATTER);
                editBirthday.setText(stringDateOfBirth);

                dayOfMonth = birthday.getDayOfMonth();
                month = birthday.getMonth().getValue();
                year = birthday.getYear();
            } else {
                var currentDateTime = LocalDateTime.now();

                dayOfMonth = currentDateTime.getDayOfMonth();
                month = currentDateTime.getMonth().getValue();
                year = currentDateTime.getYear();
            }

            mBinding.datePicker.updateDate(year, month - 1, dayOfMonth);
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

    private void navigateToEditProfile() {
        var navOptions = new NavOptions.Builder().setPopUpTo(R.id.userEditDetailsFragment, true).build();
        navController.navigate(R.id.editBirthdayFragment, null, navOptions);
        requireActivity().onBackPressed();
    }

    @Override
    public void onDestroy() {
        mViewModel.resetUpdateResult();
        super.onDestroy();
    }
}