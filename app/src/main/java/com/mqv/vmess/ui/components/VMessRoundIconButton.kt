package com.mqv.vmess.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomRoundIconButtonBinding
import com.mqv.vmess.ui.data.RoundedIconButton

typealias ButtonClickListener = (view: View) -> Unit

class VMessRoundIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleArray: Int = 0
) : ConstraintLayout(context, attrs, defStyleArray) {
    private val mBinding =
        CustomRoundIconButtonBinding.bind(inflate(context, R.layout.custom_round_icon_button, this))
    private var mListener: ButtonClickListener? = null

    init {
        mBinding.button.setOnClickListener {
            mListener?.invoke(it)
        }
    }

    fun setButton(data: RoundedIconButton?) {
        data?.let {
            with(mBinding) {
                button.icon = ContextCompat.getDrawable(context, data.icon)
                textName.text = context.getString(data.name)
            }
        }
    }

    fun setButton(@DrawableRes icon: Int) {
        mBinding.button.icon = ContextCompat.getDrawable(context, icon)
    }

    fun setButtonSize(width: Int, height: Int, iconSize: Int) {
        mBinding.button.layoutParams.width = width
        mBinding.button.layoutParams.height = height
        mBinding.button.iconSize = iconSize
        mBinding.button.requestLayout()
    }

    fun setOnButtonClickListener(listener: ButtonClickListener) {
        mListener = listener
    }

    fun hideButtonName() {
        mBinding.textName.visibility = View.GONE
    }

    fun hideButton() {
        mBinding.button.visibility = View.GONE
    }
}