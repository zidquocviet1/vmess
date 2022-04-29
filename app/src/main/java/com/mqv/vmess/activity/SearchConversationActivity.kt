package com.mqv.vmess.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ActivitySearchConversationBinding
import com.mqv.vmess.ui.fragment.SearchResultFragment
import com.mqv.vmess.ui.fragment.SuggestionFriendListFragment
import com.mqv.vmess.util.ServiceUtil.getInputMethodManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchConversationActivity :
    BaseActivity<AndroidViewModel?, ActivitySearchConversationBinding>(), TextWatcher,
    SuggestionFriendListFragment.SearchHandler, NavController.OnDestinationChangedListener {

    private lateinit var mFragment: SuggestionFriendListFragment
    private lateinit var mNavController: NavController
    private lateinit var mNavHostFragment: NavHostFragment

    override fun binding() {
        mBinding = ActivitySearchConversationBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel?>? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mNavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        mNavController = mNavHostFragment.navController
        mNavController.setGraph(
            R.navigation.search,
            bundleOf(SuggestionFriendListFragment.ARG_HIDE_SEARCH_BAR to true)
        )
        mFragment = getVisibleFragment() as SuggestionFriendListFragment
        mNavController.addOnDestinationChangedListener(this)

        with(mBinding) {
            editSearch.requestFocus()
            editSearch.addTextChangedListener(this@SearchConversationActivity)
            buttonDelete.visibility = View.GONE
            buttonBack.setOnClickListener { onBackPressed() }
            buttonDelete.setOnClickListener { editSearch.text.clear() }
        }
    }

    override fun setupObserver() {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
        mBinding.buttonDelete.visibility =
            if (s.isNotBlank() && s.isNotEmpty()) View.VISIBLE else View.GONE

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

    override fun onOpenConversation(userId: String) {
        getInputMethodManager(this).hideSoftInputFromWindow(
            mBinding.editSearch.windowToken,
            0
        )

        mFragment.insertRecentSearchPeople(userId)

        mNavController.navigate(
            R.id.conversation_activity,
            bundleOf(ConversationActivity.EXTRA_PARTICIPANT_ID to userId)
        )
    }

    private fun getVisibleFragment(): Fragment {
        return mNavHostFragment.childFragmentManager.fragments[0]
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
}