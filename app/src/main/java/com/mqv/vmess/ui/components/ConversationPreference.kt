package com.mqv.vmess.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.mqv.vmess.R
import com.mqv.vmess.util.views.ViewUtil.px

class ConversationPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleArray: Int = 0
) : Preference(context, attrs, defStyleArray) {

    private var mTitle: String? = null
    private var mTitleRes: Int = -1
    private var mSummary: Int = -1
    private var mIcon: Int = -1
    private var mShowButton: Boolean = true

    init {
        layoutResource = R.layout.item_preference_conversation_content
    }

    fun setTitle(title: String) {
        mTitle = title
    }

    override fun setTitle(title: Int) {
        mTitleRes = title
    }

    override fun setSummary(summary: Int) {
        mSummary = summary
    }

    override fun setIcon(icon: Int) {
        mIcon = icon
    }

    fun setButtonVisibility(isShow: Boolean) {
        mShowButton = isShow
    }

    fun setSummaryAndNotify(summary: Int) {
        mSummary = summary
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val title = holder.findViewById(R.id.title) as TextView
        val summary = holder.findViewById(R.id.summary) as TextView
        val button = holder.findViewById(R.id.vmess_button) as VMessRoundIconButton

        title.text = if (mTitleRes != -1) context.getString(mTitleRes) else mTitle
        button.setButton(mIcon)
        button.setButtonSize(30.px, 30.px, 18.px)
        button.hideButtonName()
        button.setOnButtonClickListener { performButtonClick() }

        if (!mShowButton) {
            button.hideButton()
        }

        if (mSummary != -1) {
            summary.text = context.getString(mSummary)
            summary.visibility = View.VISIBLE
        } else {
            summary.visibility = View.GONE
        }
    }

    private fun performButtonClick() {
        val preferenceManager = preferenceManager
        if (preferenceManager != null) {
            val listener = preferenceManager
                .onPreferenceTreeClickListener
            listener?.onPreferenceTreeClick(this)
        }
    }
}