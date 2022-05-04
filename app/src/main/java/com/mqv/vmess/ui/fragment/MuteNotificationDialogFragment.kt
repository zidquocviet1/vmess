package com.mqv.vmess.ui.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.mqv.vmess.util.AlertDialogUtil
import com.mqv.vmess.util.NavigationUtil.setNavigationResult

class MuteNotificationDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialogUtil.createMuteNotificationSelectionDialog(requireContext()) { _, which ->
            when (which) {
                0 -> setNavigationResult(
                    ConversationDetailFragment.KEY_NOTIFICATION_UNTIL,
                    (1 * 60 * 60).toLong()
                ) // 1 hour
                1 -> setNavigationResult(
                    ConversationDetailFragment.KEY_NOTIFICATION_UNTIL,
                    (8 * 60 * 60).toLong()
                ) // 8 hours
                2 -> setNavigationResult(
                    ConversationDetailFragment.KEY_NOTIFICATION_UNTIL,
                    (1 * 24 * 60 * 60).toLong()
                ) // 1 day
                3 -> setNavigationResult(
                    ConversationDetailFragment.KEY_NOTIFICATION_UNTIL,
                    (7 * 24 * 60 * 60).toLong()
                ) // 7 days
                else -> setNavigationResult(
                    ConversationDetailFragment.KEY_NOTIFICATION_UNTIL,
                    Long.MAX_VALUE
                ) // always
            }
        }
    }
}