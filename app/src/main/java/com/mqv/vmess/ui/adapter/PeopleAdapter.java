package com.mqv.vmess.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.vmess.R;
import com.mqv.vmess.databinding.ItemPeopleListBinding;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.Picture;

import java.util.function.BiConsumer;

public class PeopleAdapter extends ListAdapter<People, PeopleAdapter.PeopleViewHolder> {
    private final Context mContext;
    private final BiConsumer<Integer, Boolean> onClickConsumer;

    public PeopleAdapter(Context mContext, BiConsumer<Integer, Boolean> onClickConsumer) {
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
        this.onClickConsumer = onClickConsumer;
    }

    @NonNull
    @Override
    public PeopleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(R.layout.item_people_list, parent, false);
        return new PeopleViewHolder(ItemPeopleListBinding.bind(view), onClickConsumer);
    }

    @Override
    public void onBindViewHolder(@NonNull PeopleViewHolder holder, int position) {
        var item = getItem(position);

        holder.bindTo(item, mContext);
    }

    public static class PeopleViewHolder extends RecyclerView.ViewHolder {
        ItemPeopleListBinding mBinding;

        public PeopleViewHolder(@NonNull ItemPeopleListBinding binding, BiConsumer<Integer, Boolean> onClickConsumer) {
            super(binding.getRoot());
            mBinding = binding;

            binding.getRoot().setOnClickListener(v -> onClickConsumer.accept(getAdapterPosition(), false));
            binding.buttonError.setOnClickListener(v -> onClickConsumer.accept(getAdapterPosition(), true));
        }

        public void bindTo(People item, Context context) {
            mBinding.textDisplayName.setText(item.getDisplayName() == null ? "Test" : item.getDisplayName());
            mBinding.textUsername.setVisibility(item.getUsername() == null ? View.GONE : View.VISIBLE);
            mBinding.textUsername.setText(item.getUsername() == null ? "" : item.getUsername());

            Picture.loadUserAvatar(context, item.getPhotoUrl()).into(mBinding.imageAvatar);
        }
    }
}
