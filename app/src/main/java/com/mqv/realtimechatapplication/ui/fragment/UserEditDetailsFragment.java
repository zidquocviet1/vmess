package com.mqv.realtimechatapplication.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.EditDetailsViewModel;
import com.mqv.realtimechatapplication.databinding.FragmentUserEditDetailsBinding;
import com.mqv.realtimechatapplication.network.model.User;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class UserEditDetailsFragment extends BaseFragment<EditDetailsViewModel, FragmentUserEditDetailsBinding> implements View.OnClickListener {
    private EditText edtName, edtGender, edtBirthday, edtCurrentAddress, edtFrom;
    private NavController navController;
    private static final DateTimeFormatter NORMAL_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    public UserEditDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentUserEditDetailsBinding.inflate(inflater, container, false);
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

        edtName = Objects.requireNonNull(mBinding.textLayoutDisplayName.getEditText());
        edtGender = Objects.requireNonNull(mBinding.textLayoutGender.getEditText());
        edtBirthday = Objects.requireNonNull(mBinding.textLayoutBirthday.getEditText());
        edtCurrentAddress = Objects.requireNonNull(mBinding.textLayoutCurrentAddress.getEditText());
        edtFrom = Objects.requireNonNull(mBinding.textLayoutComeFrom.getEditText());

        edtName.setOnClickListener(this);
        edtGender.setOnClickListener(this);
        edtBirthday.setOnClickListener(this);
        edtCurrentAddress.setOnClickListener(this);
        edtFrom.setOnClickListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getFirebaseUser().observe(requireActivity(), this::showFirebaseUserUi);

        mViewModel.getLoggedInUser().observe(requireActivity(), this::showLoggedInUserUi);
    }

    private void showFirebaseUserUi(FirebaseUser user){
        if (user == null) return;

        edtName.setText(user.getDisplayName());
    }

    private void showLoggedInUserUi(User user){
        if (user == null) return;

        edtGender.setText(user.getGender() != null ? user.getGender().getValue() : "");
        edtCurrentAddress.setText("Not yet handled");
        edtFrom.setText("Not yet handled");
        edtBirthday.setText(user.getGender() != null ? user.getBirthday().format(NORMAL_FORMATTER) : "");
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();
        if (id == edtName.getId()){
            navController.navigate(R.id.action_userEditDetailsFragment_to_editDisplayNameFragment);
        }
    }
}