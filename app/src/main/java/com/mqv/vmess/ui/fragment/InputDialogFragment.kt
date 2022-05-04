package com.mqv.vmess.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mqv.vmess.R
import com.mqv.vmess.databinding.DialogEnterOtpCodeBinding
import com.mqv.vmess.util.NavigationUtil.setNavigationResult
import kotlinx.parcelize.Parcelize

private const val ARG_DATA = "data"
private const val ARG_CONVERSATION_NAME = "conversation_name"

class InputDialogFragment : DialogFragment() {
    private lateinit var mData: InputDialogData
    private lateinit var mConversationName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mData = it.getParcelable(ARG_DATA)!!
            mConversationName = it.getString(ARG_CONVERSATION_NAME)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_enter_otp_code, null, false)
        val binding: DialogEnterOtpCodeBinding = DialogEnterOtpCodeBinding.bind(view)

        binding.textTitle.setText(mData.title)
        binding.textSubtitle.setText(mData.message)
        binding.buttonDone.isEnabled = false
        binding.root.setPadding(30, 30, 30, 30)
        binding.editTextOtp.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.editTextOtp.filters = arrayOf(InputFilter.LengthFilter(50))
        binding.editTextOtp.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun afterTextChanged(editable: Editable) {
                    binding.buttonDone.isEnabled = editable.isNotEmpty()
                }
            })

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .show()

        binding.buttonCancel.setOnClickListener { _ -> dialog.dismiss() }
        binding.buttonDone.setOnClickListener { _ ->
            val oldName = mConversationName
            val newName = binding.editTextOtp.text.toString().trim()
            if (oldName != newName) {
                setNavigationResult(ConversationDetailFragment.KEY_GROUP_NAME, newName)
            }
            dialog.dismiss()
        }

        return dialog
    }

    companion object {
        fun newInstance(data: InputDialogData, conversationName: String) {
            InputDialogFragment().apply {
                arguments = bundleOf(
                    ARG_DATA to data,
                    ARG_CONVERSATION_NAME to conversationName
                )
            }
        }
    }
}

@Parcelize
data class InputDialogData(
    val title: Int,
    val message: Int
) : Parcelable