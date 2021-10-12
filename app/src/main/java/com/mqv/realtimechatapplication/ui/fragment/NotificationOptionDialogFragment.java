package com.mqv.realtimechatapplication.ui.fragment;

import static com.mqv.realtimechatapplication.ui.fragment.NotificationFragment.ACTION_MARK_READ;
import static com.mqv.realtimechatapplication.ui.fragment.NotificationFragment.ACTION_REMOVE;
import static com.mqv.realtimechatapplication.ui.fragment.NotificationFragment.ACTION_REPORT;
import static com.mqv.realtimechatapplication.ui.fragment.NotificationFragment.EXTRA_KEY_ACTION;
import static com.mqv.realtimechatapplication.ui.fragment.NotificationFragment.EXTRA_NOTIFICATION;
import static com.mqv.realtimechatapplication.ui.fragment.NotificationFragment.REQUEST_KEY;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.FragmentNotificationOptionDialogBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Picture;

public class NotificationOptionDialogFragment extends BottomSheetDialogFragment {
    private FragmentNotificationOptionDialogBinding mBinding;
    private Notification mNotification;

    private static final String NOTIFICATION = "notification";

    public static NotificationOptionDialogFragment newInstance(Notification notification) {
        var fragment = new NotificationOptionDialogFragment();
        var args = new Bundle();
        args.putParcelable(NOTIFICATION, notification);
        fragment.setArguments(args);
        return fragment;
    }

    public NotificationOptionDialogFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentNotificationOptionDialogBinding.inflate(inflater, container, false);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            setupUI(getArguments());
            registerEventClick();
        } else {
            throw new IllegalArgumentException("The instance arguments must not be null");
        }
    }

    @Override
    public int getTheme() {
        return R.style.UserSocialLinkListDialogFragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void setupUI(Bundle data) {
        var item = (Notification) data.getParcelable(NOTIFICATION);
        mNotification = item;

        mBinding.textBody.setText(Html.fromHtml(item.getBody(), Html.FROM_HTML_MODE_COMPACT));

        mBinding.itemRemove.icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_delete));
        mBinding.itemRemove.title.setText(R.string.title_remove_notification);
        mBinding.itemRemove.title.setTypeface(Typeface.DEFAULT_BOLD);
        mBinding.itemRemove.summary.setVisibility(View.GONE);

        mBinding.itemReportProblem.title.setText(R.string.title_report_notification);
        mBinding.itemReportProblem.title.setTypeface(Typeface.DEFAULT_BOLD);
        mBinding.itemReportProblem.summary.setVisibility(View.GONE);
        mBinding.itemReportProblem.icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_report));

        mBinding.itemMarkRead.title.setText(R.string.title_mark_read_notification);
        mBinding.itemMarkRead.title.setTypeface(Typeface.DEFAULT_BOLD);
        mBinding.itemMarkRead.summary.setVisibility(View.GONE);
        mBinding.itemMarkRead.icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_mark_read));

        var formatUrl = item.getAgentImageUrl() == null ? null :
                item.getAgentImageUrl().replace("localhost", Const.BASE_IP);

        GlideApp.with(this)
                .load(formatUrl)
                .placeholder(Picture.getDefaultCirclePlaceHolder(requireContext()))
                .fallback(R.drawable.ic_round_account)
                .error(R.drawable.ic_account_undefined)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(mBinding.image);
    }

    private void registerEventClick() {
        mBinding.itemRemove.getRoot().setOnClickListener(v -> {
            var data = new Bundle();
            data.putString(EXTRA_KEY_ACTION, ACTION_REMOVE);
            data.putParcelable(EXTRA_NOTIFICATION, mNotification);
            requireActivity().getSupportFragmentManager().setFragmentResult(REQUEST_KEY, data);
            dismiss();
        });
        mBinding.itemReportProblem.getRoot().setOnClickListener(v -> {
            var data = new Bundle();
            data.putString(EXTRA_KEY_ACTION, ACTION_REPORT);
            data.putParcelable(EXTRA_NOTIFICATION, mNotification);
            requireActivity().getSupportFragmentManager().setFragmentResult(REQUEST_KEY, data);
            dismiss();
        });
        mBinding.itemMarkRead.getRoot().setOnClickListener(v -> {
            var data = new Bundle();
            data.putString(EXTRA_KEY_ACTION, ACTION_MARK_READ);
            data.putParcelable(EXTRA_NOTIFICATION, mNotification);
            requireActivity().getSupportFragmentManager().setFragmentResult(REQUEST_KEY, data);
            dismiss();
        });
    }
}
