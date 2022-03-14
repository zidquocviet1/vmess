package com.mqv.vmess.ui.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import com.mqv.vmess.R;

public abstract class BaseSwipeFragment<V extends ViewModel, B extends ViewBinding> extends BaseFragment<V, B>
        implements SwipeRefreshLayout.OnRefreshListener {
    private SwipeRefreshLayout srl;
    private final int[] indicatorColorArr = {R.color.purple_500};

    @Nullable
    public abstract SwipeRefreshLayout getSwipeLayout();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getSwipeLayout() != null) {
            srl = getSwipeLayout();
            srl.setOnRefreshListener(this);
            srl.setColorSchemeResources(indicatorColorArr);
        }
    }

    public void stopRefresh(){
        if (srl != null && srl.isRefreshing()){
            srl.setRefreshing(false);
        }
    }
}
