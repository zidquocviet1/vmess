package com.mqv.vmess.activity

import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ActivityForwardMessageBinding
import com.mqv.vmess.ui.fragment.SuggestionFriendListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForwardMessageActivity : ToolbarActivity<AndroidViewModel, ActivityForwardMessageBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        updateActionBarTitle(R.string.title_send_to)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_suggested_friend, SuggestionFriendListFragment.newInstance(singleSend = true, includeGroup = true))
            .commit()
    }

    override fun binding() {
        mBinding = ActivityForwardMessageBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel>? = null

    override fun setupObserver() {

    }
}