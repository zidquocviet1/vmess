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
import com.mqv.realtimechatapplication.activity.viewmodel.EditDetailsViewModel;
import com.mqv.realtimechatapplication.databinding.FragmentUserEditDetailsBinding;
import com.mqv.realtimechatapplication.network.model.User;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class UserEditDetailsFragment extends BaseFragment<EditDetailsViewModel, FragmentUserEditDetailsBinding>
        implements View.OnClickListener {
    private EditText edtName, edtGender, edtBirthday, edtCurrentAddress, edtFrom;
    private NavController navController;
    private String currentDisplayName;
    public static final DateTimeFormatter NORMAL_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

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

        mViewModel.getDisplayName().observe(requireActivity(), name -> currentDisplayName = name);
    }

    private void showFirebaseUserUi(FirebaseUser user){
        if (user == null) return;

        edtName.setText(user.getDisplayName());
    }

    private void showLoggedInUserUi(User user){
        if (user == null) return;

        edtGender.setText(user.getGender() != null ? user.getGender().getValue(requireContext()) : "");
        edtCurrentAddress.setText("Not yet handled");
        edtFrom.setText("Not yet handled");
        edtBirthday.setText(user.getBirthday() != null ? user.getBirthday().format(NORMAL_FORMATTER) : "");
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();
        if (id == edtName.getId()){
            var editDisplayNameAction =
                    UserEditDetailsFragmentDirections.actionEditDisplayNameFragment();
            editDisplayNameAction.setDisplayName(currentDisplayName);
            navController.navigate(editDisplayNameAction);
        }else if (id == edtGender.getId()){
            var editGenderAction = UserEditDetailsFragmentDirections.actionEditGender();
            /*
            * Why we do not use the Enum Safe Args?
            * Because the Nav Component does not allow the null Enum value. So we must change it to Integer and get it by key itself
            * */
//            editGenderAction.setGenderKey(1);
            navController.navigate(editGenderAction);
        }else if (id == edtBirthday.getId()){
            var editBirthdayAction = UserEditDetailsFragmentDirections.actionEditBirthday();
            navController.navigate(editBirthdayAction);
        }
    }
}