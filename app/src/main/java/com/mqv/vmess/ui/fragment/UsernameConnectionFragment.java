package com.mqv.vmess.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.RequestPeopleActivity;
import com.mqv.vmess.activity.viewmodel.ConnectPeopleViewModel;
import com.mqv.vmess.databinding.FragmentUsernameConnectionBinding;
import com.mqv.vmess.util.NetworkStatus;

public class UsernameConnectionFragment extends BaseFragment<ConnectPeopleViewModel, FragmentUsernameConnectionBinding>
        implements TextWatcher, View.OnClickListener {
    private static final int MAX_USER_NAME_LENGTH = 20;

    public UsernameConnectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentUsernameConnectionBinding.inflate(inflater, container, false);
    }

    @Override
    public Class<ConnectPeopleViewModel> getViewModelClass() {
        return ConnectPeopleViewModel.class;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.textPromptLength.setText(getString(R.string.prompt_bio_length, 0, MAX_USER_NAME_LENGTH));
        mBinding.editUserName.addTextChangedListener(this);
        mBinding.buttonBack.setOnClickListener(this);
        mBinding.buttonFind.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.removeUsernameDisposable();
    }

    @Override
    public void setupObserver() {
        mViewModel.getConnectUserResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            var status = result.getStatus();
            var isLoading = status == NetworkStatus.LOADING;

            mBinding.progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            mBinding.buttonFind.setEnabled(!isLoading);
            mBinding.editUserName.setEnabled(!isLoading);

            switch (status) {
                case SUCCESS:
                    var user = result.getSuccess();
                    var firebaseUser = mViewModel.getFirebaseUser().getValue();

                    if (firebaseUser != null) {
                        if (user.getUid().equals(firebaseUser.getUid())) {
                            Toast.makeText(requireContext(), R.string.msg_request_yourself, Toast.LENGTH_SHORT).show();
                        } else {
                            var intent = new Intent(requireActivity(), RequestPeopleActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                            requireActivity().finish();
                        }
                    }
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }

            mViewModel.resetConnectUserResult();
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        mBinding.buttonFind.setEnabled(s.length() != 0);
        mBinding.textPromptLength.setText(getString(R.string.prompt_bio_length, s.length(), MAX_USER_NAME_LENGTH));
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.buttonBack.getId()) {
            requireActivity().onBackPressed();
        } else if (id == mBinding.buttonFind.getId()) {
            var username = mBinding.editUserName.getText().toString().trim();

            mViewModel.getConnectUserByUsername(username);
        }
    }
}