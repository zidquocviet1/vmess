package com.mqv.realtimechatapplication.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import com.mqv.realtimechatapplication.R;

public abstract class BaseSwipeFragment<V extends ViewModel, B extends ViewBinding> extends BaseFragment<V, B>
        implements SwipeRefreshLayout.OnRefreshListener {
    private SwipeRefreshLayout srl;
    private final int[] indicatorColorArr = {R.color.purple_500};

    @NonNull
    public abstract SwipeRefreshLayout getSwipeLayout();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        srl = getSwipeLayout();
        srl.setOnRefreshListener(this);
        srl.setColorSchemeResources(indicatorColorArr);
    }

    public void stopRefresh(){
        if (srl != null){
            srl.setRefreshing(false);
        }
    }
}
