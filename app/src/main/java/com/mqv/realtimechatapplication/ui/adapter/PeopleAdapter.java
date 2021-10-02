package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemPeopleListBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.Const;

public class PeopleAdapter extends ListAdapter<People, PeopleAdapter.PeopleViewHolder> {
    private final Context mContext;

    public PeopleAdapter(Context mContext) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull People oldItem, @NonNull People newItem) {
                return oldItem.getUid().equals(newItem.getUid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull People oldItem, @NonNull People newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public PeopleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(R.layout.item_people_list, parent, false);
        return new PeopleViewHolder(ItemPeopleListBinding.bind(view));
    }

    @Override
    public void onBindViewHolder(@NonNull PeopleViewHolder holder, int position) {
        var item = getItem(position);

        holder.bindTo(item, mContext);
    }

    public static class PeopleViewHolder extends RecyclerView.ViewHolder {
        ItemPeopleListBinding mBinding;

        public PeopleViewHolder(@NonNull ItemPeopleListBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        public void bindTo(People item, Context context) {
            mBinding.textDisplayName.setText(item.getDisplayName() == null ? "Test" : item.getDisplayName());
            mBinding.textUsername.setVisibility(item.getUsername() == null ? View.GONE : View.VISIBLE);
            mBinding.textUsername.setText(item.getUsername() == null ? "" : item.getUsername());

            var url = item.getPhotoUrl() == null ? "" : item.getPhotoUrl().replace("localhost", Const.BASE_IP);

            GlideApp.with(context)
                    .load(url)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_round_account)
                    .into(mBinding.imageAvatar);
        }
    }
}
