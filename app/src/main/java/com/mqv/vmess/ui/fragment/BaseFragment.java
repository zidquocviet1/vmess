package com.mqv.vmess.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.mqv.vmess.activity.BaseActivity;
import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.util.MyActivityForResult;

import java.util.Map;

public abstract class BaseFragment<V extends ViewModel, B extends ViewBinding> extends Fragment {
    public B mBinding;
    public V mViewModel;

    public MyActivityForResult<Intent, ActivityResult> mActivityLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.StartActivityForResult());
    public MyActivityForResult<String[], Map<String, Boolean>> mPermissionLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.RequestMultiplePermissions());

    public abstract void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    public abstract Class<V> getViewModelClass();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding(inflater, container);

        if (getViewModelClass() != null) {
            mViewModel = new ViewModelProvider(requireActivity()).get(getViewModelClass());
        }

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupObserver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    public abstract void setupObserver();

    public AppPreferences getPreference() {
        return ((BaseActivity)requireActivity()).getAppPreference();
    }
}
