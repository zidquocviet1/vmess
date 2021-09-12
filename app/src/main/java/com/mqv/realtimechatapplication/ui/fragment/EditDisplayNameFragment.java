package com.mqv.realtimechatapplication.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.EditDetailsActivity;
import com.mqv.realtimechatapplication.databinding.FragmentEditDisplayNameBinding;

import java.util.Objects;

public class EditDisplayNameFragment extends Fragment {
    private FragmentEditDisplayNameBinding mBinding;
    private NavController navController;
    private static final int MAX_NAME_LENGTH = 30;

    public EditDisplayNameFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentEditDisplayNameBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
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
                mBinding.includedBottom.buttonBottom.setEnabled(s.length() != 0);
                mBinding.textLayoutDisplayName.setHelperText(getString(R.string.prompt_bio_length, s.length(), MAX_NAME_LENGTH));
                if (s.length() == 0){
                    mBinding.textLayoutDisplayName.setError(getString(R.string.invalid_display_name));
                }
            }
        });
        editDisplayName.setText(currentName);

        mBinding.includedBottom.buttonBottom.setOnClickListener(v -> {
            var newName = editDisplayName.getText().toString().trim();

            // Return to EditProfile screen when new name are the same with old name
            if (currentName.equals(newName)) {
                navigateToEditProfile();
                return;
            }

            var firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                showLoadingUi(true);

                var request = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName).build();

                firebaseUser.updateProfile(request)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                showLoadingUi(false);

                                Toast.makeText(requireContext(), "Update display name successfully", Toast.LENGTH_SHORT).show();

                                ((EditDetailsActivity) requireActivity()).reloadFirebaseUser();

                                navigateToEditProfile();
                            } else {
                                showLoadingUi(false);

                                Toast.makeText(requireContext(), "Update display name failure", Toast.LENGTH_SHORT).show();
                            }
                        });
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
        mBinding.includedBottom.buttonBottom.setEnabled(!isLoading);
    }
}