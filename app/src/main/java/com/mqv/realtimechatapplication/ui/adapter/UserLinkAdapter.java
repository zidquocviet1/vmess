package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemPreferenceContentBinding;

public class UserLinkAdapter extends ListAdapter<String, UserLinkAdapter.UserLinkViewHolder> {
    private final Context mContext;

    public UserLinkAdapter(Context context){
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return oldItem.equals(newItem);
            }
        });
        mContext = context;
    }

    @NonNull
    @Override
    public UserLinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(R.layout.item_preference_content, parent, false);
        return new UserLinkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserLinkViewHolder holder, int position) {
        var item = getItem(position);
        holder.bind(item);
    }

    public static class UserLinkViewHolder extends RecyclerView.ViewHolder{
        ItemPreferenceContentBinding mBinding;

        public UserLinkViewHolder(@NonNull View itemView) {
            super(itemView);
            mBinding = ItemPreferenceContentBinding.bind(itemView);
        }

        public void bind(String item){
            mBinding.title.setText(item);
            mBinding.summary.setVisibility(View.GONE);
        }
    }
}
