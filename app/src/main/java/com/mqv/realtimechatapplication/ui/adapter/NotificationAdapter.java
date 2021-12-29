package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemNotificationFragmentBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.di.GlideRequests;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.util.Picture;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {
    private final Context mContext;
    private List<Notification> mData;
    private Consumer<Integer> onItemClick;
    private Consumer<Integer> onChangeItem;
    private Consumer<Boolean> onDatasetChange;
    private static final String WEEK_PATTERN = "EEE hh:mm a";
    private static final String MONTH_PATTERN = "MMM dd hh:mm a";

    public NotificationAdapter(Context context, List<Notification> data) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                return oldItem.equals(newItem);
            }
        });
        mContext = context;
        mData = data;
    }

    public void setOnItemClick(Consumer<Integer> onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void setOnChangeItem(Consumer<Integer> onChangeItem) {
        this.onChangeItem = onChangeItem;
    }

    public void setOnDatasetChange(Consumer<Boolean> onDatasetChange) {
        this.onDatasetChange = onDatasetChange;
    }

    public void removeItem(Notification notification) {
        var index = mData.indexOf(notification);

        mData.remove(index);

        notifyItemRemoved(index);
        notifyItemRangeChanged(index, mData.size());

        if (onDatasetChange != null)
            onDatasetChange.accept(mData.isEmpty());
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(R.layout.item_notification_fragment, parent, false);
        return new NotificationViewHolder(ItemNotificationFragmentBinding.bind(view),
                mContext,
                GlideApp.with(mContext),
                onItemClick,
                onChangeItem);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ItemNotificationFragmentBinding mBinding;
        Context mContext;
        GlideRequests mRequest;

        public NotificationViewHolder(@NonNull ItemNotificationFragmentBinding binding,
                                      Context context,
                                      GlideRequests request,
                                      Consumer<Integer> onItemClick,
                                      Consumer<Integer> onChangeItem) {
            super(binding.getRoot());
            mBinding = binding;
            mContext = context;
            mRequest = request;

            binding.getRoot().setOnClickListener(v -> {
                if (onItemClick != null)
                    onItemClick.accept(getAdapterPosition());
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (onChangeItem != null)
                    onChangeItem.accept(getAdapterPosition());

                return true;
            });

            binding.iconMoreHorizontal.setOnClickListener(v -> {
                if (onChangeItem != null)
                    onChangeItem.accept(getAdapterPosition());
            });
        }

        public void bindTo(Notification item) {
            mBinding.textBody.setText(Html.fromHtml(item.getBody(), Html.FROM_HTML_MODE_COMPACT));
            mBinding.textTimestamp.setText(convertReadableTimestamp(item.getCreatedDate()));
            mBinding.layoutUnread.setVisibility(item.getHasRead() ? View.GONE : View.VISIBLE);

            Picture.loadUserAvatar(mContext, item.getAgentImageUrl())
                   .transition(DrawableTransitionOptions.withCrossFade())
                   .into(mBinding.imageAvatar);
        }

        private String convertReadableTimestamp(LocalDateTime time) {
            var now = LocalDateTime.now();

            var minute = ChronoUnit.MINUTES.between(time, now);
            var hour = ChronoUnit.HOURS.between(time, now);
            var day = ChronoUnit.DAYS.between(time, now);

            if (minute == 0)
                return mContext.getString(R.string.msg_notification_now);

            if (minute >= 1 && minute <= 60)
                return mContext.getString(R.string.msg_notification_minutes, minute);

            if (day <= 0) {
                return mContext.getString(R.string.msg_notification_hours, hour);
            }

            if (day == 1) {
                var arr = time.format(DateTimeFormatter.ofPattern(WEEK_PATTERN)).split(" ");

                return mContext.getString(R.string.msg_notification_yesterday, arr[1] + " " + arr[2]);
            }

            if (day <= 7) {
                var arr = time.format(DateTimeFormatter.ofPattern(WEEK_PATTERN)).split(" ");

                return mContext.getString(R.string.msg_notification_week, arr[0], arr[1] + " " + arr[2]);
            } else {
                var arr = time.format(DateTimeFormatter.ofPattern(MONTH_PATTERN)).split(" ");

                return mContext.getString(R.string.msg_notification_month, arr[0], arr[1], arr[2] + " " + arr[3]);
            }
        }
    }
}
