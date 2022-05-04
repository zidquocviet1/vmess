package com.mqv.vmess.ui.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mqv.vmess.R
import com.mqv.vmess.activity.ConversationDetailActivity
import com.mqv.vmess.ui.data.AlertDialogData
import com.mqv.vmess.ui.data.Type
import com.mqv.vmess.util.NavigationUtil.setNavigationResult

private const val ARG_DATA = "data"

class VMessAlertDialogFragment : DialogFragment() {
    private lateinit var mData: AlertDialogData
    private var mRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mData = it.getParcelable(ARG_DATA)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(mData.title)
            if (mData.message != -1) {
                setMessage(mData.message)
            }
            setPositiveButton(mData.positiveButton) { dialog, _ ->
                dialog.dismiss()

                mRunnable?.run()

                when (mData.type) {
                    Type.DELETE -> setNavigationResult(
                        ConversationDetailActivity.KEY_DELETE_CONVERSATION,
                        true
                    )
                    Type.LEAVE_GROUP -> setNavigationResult(
                        ConversationDetailActivity.KEY_LEAVE_GROUP,
                        true
                    )
                }
            }
            setNegativeButton(mData.negativeButton, null)
            background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.background_alert_dialog_corner_radius
            )
        }.create()
    }

    fun setOnPositiveClickListener(runnable: Runnable): VMessAlertDialogFragment {
        mRunnable = runnable
        return this
    }

    companion object {
        fun newInstance(data: AlertDialogData) =
            VMessAlertDialogFragment().apply {
                arguments = bundleOf(
                    ARG_DATA to data
                )
            }
    }
}