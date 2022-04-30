package com.mqv.vmess.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ActivityAddConversationBinding
import com.mqv.vmess.ui.fragment.SearchResultFragment
import com.mqv.vmess.ui.fragment.SuggestionFriendListFragment
import com.mqv.vmess.util.ServiceUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddConversationActivity :
    ToolbarActivity<AndroidViewModel?, ActivityAddConversationBinding>(), TextWatcher,
    NavController.OnDestinationChangedListener, SuggestionFriendListFragment.SearchHandler {

    private lateinit var mNavHost: NavHostFragment
    private lateinit var mNavController: NavController
    private lateinit var mFragment: SuggestionFriendListFragment

    override fun binding() {
        mBinding = ActivityAddConversationBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel?>? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        updateActionBarTitle(R.string.label_new_message)

        mNavHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        mNavController = mNavHost.navController
        mNavController.setGraph(
            R.navigation.search,
            bundleOf(
                SuggestionFriendListFragment.ARG_HIDE_SEARCH_BAR to true,
                SuggestionFriendListFragment.ARG_HIDE_RECENT_SEARCH to true
            )
        )
        mNavController.addOnDestinationChangedListener(this)
        mFragment = getVisibleFragment() as SuggestionFriendListFragment

        mBinding.layoutNewGroup.setOnClickListener {
            activityResultLauncher.launch(
                Intent(
                    this,
                    AddGroupConversationActivity::class.java
                )
            ) { result ->
                result?.let {
                    if (result.resultCode == RESULT_OK) {
                        setResult(RESULT_OK, result.data)
                        finish()
                    }
                }
            }
        }

        mBinding.editSearch.addTextChangedListener(this)
    }

    override fun setupObserver() {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
        if (s.isNotEmpty()) {
            mNavController.navigate(
                R.id.searchResultFragment,
                bundleOf(SearchResultFragment.EXTRA_NAME to s.toString()),
                NavOptions.Builder().setLaunchSingleTop(true).build()
            )
            val searchResultFragment = getVisibleFragment()
            if (searchResultFragment is SearchResultFragment) {
                searchResultFragment.postNewNameRequest(s.toString())
            }
        } else {
            if (mNavController.currentDestination?.id == R.id.suggestionFriendListFragment) {
                return
            }
            mNavController.popBackStack(R.id.suggestionFriendListFragment, false)
        }
    }

    private fun getVisibleFragment(): Fragment {
        return mNavHost.childFragmentManager.fragments[0]
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        if (destination.id == R.id.suggestionFriendListFragment) {
            mBinding.editSearch.text.clear()
        }
    }

    override fun onOpenConversation(userId: String) {
        ServiceUtil.getInputMethodManager(this).hideSoftInputFromWindow(
            mBinding.editSearch.windowToken,
            0
        )

        mFragment.insertRecentSearchPeople(userId)

        mNavController.navigate(
            R.id.conversation_activity,
            bundleOf(ConversationActivity.EXTRA_PARTICIPANT_ID to userId)
        )
    }
}