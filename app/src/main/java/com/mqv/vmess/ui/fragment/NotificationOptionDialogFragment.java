package com.mqv.vmess.ui.fragment;

import static com.mqv.vmess.ui.fragment.NotificationFragment.ACTION_MARK_READ;
import static com.mqv.vmess.ui.fragment.NotificationFragment.ACTION_REMOVE;
import static com.mqv.vmess.ui.fragment.NotificationFragment.ACTION_REPORT;
import static com.mqv.vmess.ui.fragment.NotificationFragment.EXTRA_KEY_ACTION;
import static com.mqv.vmess.ui.fragment.NotificationFragment.EXTRA_NOTIFICATION;
import static com.mqv.vmess.ui.fragment.NotificationFragment.REQUEST_KEY;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mqv.vmess.R;
import com.mqv.vmess.data.model.FriendNotificationType;
import com.mqv.vmess.databinding.FragmentNotificationOptionDialogBinding;
import com.mqv.vmess.ui.data.FriendNotificationState;
import com.mqv.vmess.util.Picture;

public class NotificationOptionDialogFragment extends BottomSheetDialogFragment {
    private FragmentNotificationOptionDialogBinding mBinding;
    private FriendNotificationState mNotification;

    private static final String NOTIFICATION = "notification";

    public static NotificationOptionDialogFragment newInstance(FriendNotificationState notification) {
        NotificationOptionDialogFragment fragment = new NotificationOptionDialogFragment();
        Bundle args = new Bundle();
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
        mNotification = (FriendNotificationState) data.getParcelable(NOTIFICATION);

        mBinding.textBody.setText(getBodyText(mNotification));

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

        Picture.loadUserAvatar(this, mNotification.getSender().getPhotoUrl()).into(mBinding.image);
    }

    private void registerEventClick() {
        mBinding.itemRemove.getRoot().setOnClickListener(v -> returnFragmentResult(ACTION_REMOVE));
        mBinding.itemReportProblem.getRoot().setOnClickListener(v -> returnFragmentResult(ACTION_REPORT));
        mBinding.itemMarkRead.getRoot().setOnClickListener(v -> returnFragmentResult(ACTION_MARK_READ));
    }

    private void returnFragmentResult(String action) {
        Bundle data = new Bundle();

        data.putString(EXTRA_KEY_ACTION, action);
        data.putParcelable(EXTRA_NOTIFICATION, mNotification);

        requireActivity().getSupportFragmentManager().setFragmentResult(REQUEST_KEY, data);
        dismiss();
    }

    private Spanned getBodyText(FriendNotificationState item) {
        String result;

        if (item.getType() == FriendNotificationType.ACCEPTED_FRIEND) {
            result = getString(R.string.msg_accepted_friend_request_notification_fragment, item.getSender().getDisplayName());
        } else if (item.getType() == FriendNotificationType.REQUEST_FRIEND) {
            result = getString(R.string.msg_new_friend_request_notification_fragment, item.getSender().getDisplayName());
        } else {
            result = "Unknown";
        }
        return Html.fromHtml(result, Html.FROM_HTML_MODE_COMPACT);
    }
}
