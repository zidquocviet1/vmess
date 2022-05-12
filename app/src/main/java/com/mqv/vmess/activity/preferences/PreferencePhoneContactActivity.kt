package com.mqv.vmess.activity.preferences

import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import com.mqv.vmess.R
import com.mqv.vmess.activity.ToolbarActivity
import com.mqv.vmess.databinding.ActivityPreferencePhoneContactBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreferencePhoneContactActivity : ToolbarActivity<AndroidViewModel, ActivityPreferencePhoneContactBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        updateActionBarTitle(R.string.title_preference_item_phone_contacts)
    }

    override fun binding() {
        mBinding = ActivityPreferencePhoneContactBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel>? = null

    override fun setupObserver() {
    }
}