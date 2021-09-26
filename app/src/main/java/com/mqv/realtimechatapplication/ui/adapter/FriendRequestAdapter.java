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
import com.bumptech.glide.signature.ObjectKey;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemFriendRequestBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.network.model.FriendRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public class FriendRequestAdapter extends ListAdapter<FriendRequest, FriendRequestAdapter.FriendRequestVH> {
    private final Context mContext;
    private final List<FriendRequest> mMutableListItem;
    private Consumer<Integer> onRequestClicked;
    private final Consumer<Integer> onConfirmClicked;
    private final Consumer<Integer> onCancelClicked;

    public FriendRequestAdapter(Context context,
                                List<FriendRequest> listItem,
                                Consumer<Integer> onConfirmClicked,
                                Consumer<Integer> onCancelClicked) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull FriendRequest oldItem, @NonNull FriendRequest newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull FriendRequest oldItem, @NonNull FriendRequest newItem) {
                return false;
            }
        });
        mContext = context;
        mMutableListItem = listItem;
        this.onConfirmClicked = onConfirmClicked;
        this.onCancelClicked = onCancelClicked;
    }

    public void setOnRequestClicked(Consumer<Integer> callback) {
        this.onRequestClicked = callback;
    }

    public void removeItem(int position) {
        mMutableListItem.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mMutableListItem.size());
    }

    @NonNull
    @Override
    public FriendRequestVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestVH(view, onRequestClicked, onConfirmClicked, onCancelClicked);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestVH holder, int position) {
        var item = getItem(position);
        holder.bindTo(item, mContext);
    }

    public static class FriendRequestVH extends RecyclerView.ViewHolder {
        ItemFriendRequestBinding binding;

        public FriendRequestVH(@NonNull View itemView, Consumer<Integer> callback,
                               Consumer<Integer> onConfirmClicked, Consumer<Integer> onCancelClicked) {
            super(itemView);
            binding = ItemFriendRequestBinding.bind(itemView);
            binding.buttonConfirm.setOnClickListener(v -> onConfirmClicked.accept(getAdapterPosition()));
            binding.buttonCancel.setOnClickListener(v -> onCancelClicked.accept(getAdapterPosition()));
            itemView.setOnClickListener(v -> {
                if (callback != null)
                    callback.accept(getAdapterPosition());
            });
        }

        public void bindTo(FriendRequest item, Context mContext) {
            GlideApp.with(mContext)
                    .load(item.getPhotoUrl())
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .signature(new ObjectKey(item.getPhotoUrl()))
                    .error(R.drawable.ic_round_account)
                    .into(binding.imageAvatar);
            binding.title.setText(item.getDisplayName());
            binding.textTimestamp.setText(specificTimeStamp(item.getCreatedDate()));
        }

        private String specificTimeStamp(LocalDateTime from) {
            var now = LocalDateTime.now();

            var year = ChronoUnit.YEARS.between(from, now);
            var weekOfYear = ChronoUnit.WEEKS.between(from, now);
            var day = ChronoUnit.DAYS.between(from, now);
            var hour = ChronoUnit.HOURS.between(from, now);
            var minute = ChronoUnit.MINUTES.between(from, now);

            if (year > 0)
                return year + "y";

            if (weekOfYear > 0)
                return weekOfYear + "w";

            if (day > 0)
                return day + "d";

            if (hour > 0)
                return hour + "h";

            if (minute > 0)
                return minute + "m";

            return "now";
        }
    }
}