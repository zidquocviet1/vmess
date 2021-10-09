package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.databinding.ItemAddLoggedInUserBinding;
import com.mqv.realtimechatapplication.databinding.ItemLoggedInUserBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.util.Const;

import java.util.List;
import java.util.function.Consumer;

public class LoggedInUserAdapter extends ListAdapter<HistoryLoggedInUser, RecyclerView.ViewHolder> {
    private final List<HistoryLoggedInUser> mMutableListUser;
    private final Context mContext;
    private Consumer<Void> onAddAccountClick;
    private Consumer<HistoryLoggedInUser> onChangeAccountClick;
    private Consumer<HistoryLoggedInUser> onRemoveUser;

    public LoggedInUserAdapter(Context context, @NonNull List<HistoryLoggedInUser> listLoggedInUser) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull HistoryLoggedInUser oldItem, @NonNull HistoryLoggedInUser newItem) {
                return oldItem.getUid().equals(newItem.getUid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull HistoryLoggedInUser oldItem, @NonNull HistoryLoggedInUser newItem) {
                return false;
            }
        });
        mContext = context;
        mMutableListUser = listLoggedInUser;
        submitList(listLoggedInUser);
    }

    public void setOnAddAccountClick(Consumer<Void> onAddAccountClick) {
        this.onAddAccountClick = onAddAccountClick;
    }

    public void setOnChangeAccountClick(Consumer<HistoryLoggedInUser> onChangeAccountClick) {
        this.onChangeAccountClick = onChangeAccountClick;
    }

    public void setOnRemoveUser(Consumer<HistoryLoggedInUser> onRemoveUser) {
        this.onRemoveUser = onRemoveUser;
    }

    public void removeItem(int position) {
        var user = mMutableListUser.get(position);

        mMutableListUser.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mMutableListUser.size());

        if (onRemoveUser != null)
            onRemoveUser.accept(user);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if ((viewType + 1) == mMutableListUser.size()) {
            return new AddLoggedInUserViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_add_logged_in_user, parent, false));
        } else {
            return new LoggedInUserViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_logged_in_user, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        var type = getItemViewType(position);

        if ((type + 1) == mMutableListUser.size()) {
            var addAccountHolder = (AddLoggedInUserViewHolder) holder;
            addAccountHolder.itemView.setOnClickListener(v -> {
                if (onAddAccountClick != null)
                    onAddAccountClick.accept(null);
            });
        } else {
            var item = getItem(position);
            var loggedInHolder = (LoggedInUserViewHolder) holder;
            loggedInHolder.bindTo(item, mContext);
            loggedInHolder.itemView.setOnClickListener(v -> {
                if (onChangeAccountClick != null)
                    onChangeAccountClick.accept(item);
            });
        }
    }

    public static class LoggedInUserViewHolder extends RecyclerView.ViewHolder {
        ItemLoggedInUserBinding binding;

        public LoggedInUserViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemLoggedInUserBinding.bind(itemView);
        }

        public void bindTo(HistoryLoggedInUser user, Context context) {
            var photoUrl = user.getPhotoUrl() == null ? null : user.getPhotoUrl().replace("localhost", Const.BASE_IP);

            var placeHolder = new CircularProgressDrawable(context);
            placeHolder.setStrokeWidth(5f);
            placeHolder.setCenterRadius(30f);
            placeHolder.start();

            GlideApp.with(context)
                    .load(photoUrl)
                    .placeholder(placeHolder)
                    .error(ContextCompat.getDrawable(context, R.drawable.ic_account_undefined))
                    .fallback(ContextCompat.getDrawable(context, R.drawable.ic_account_undefined))
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.imageAvatar);

            binding.title.setText(user.getDisplayName());
            binding.iconCheck.setVisibility(user.getLogin() ? View.VISIBLE : View.GONE);
            binding.summary.setVisibility(user.getLogin() ? View.VISIBLE : View.GONE);
        }
    }

    public static class AddLoggedInUserViewHolder extends RecyclerView.ViewHolder {
        ItemAddLoggedInUserBinding binding;

        public AddLoggedInUserViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemAddLoggedInUserBinding.bind(itemView);
        }
    }
}
