package com.mqv.realtimechatapplication.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.chip.Chip
import com.mqv.realtimechatapplication.R
import com.mqv.realtimechatapplication.activity.viewmodel.UserSelectionListViewModel
import com.mqv.realtimechatapplication.databinding.ActivityAddGroupConversationBinding
import com.mqv.realtimechatapplication.ui.GlideChip
import com.mqv.realtimechatapplication.ui.data.UserSelection
import com.mqv.realtimechatapplication.ui.fragment.ConversationListFragment
import com.mqv.realtimechatapplication.ui.fragment.SuggestionFriendListFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.stream.Collectors

const val PARTICIPANTS_THRESHOLD = 1

@AndroidEntryPoint
class AddGroupConversationActivity :
    ToolbarActivity<UserSelectionListViewModel, ActivityAddGroupConversationBinding>() {

    private lateinit var fragment: SuggestionFriendListFragment
    private val mSelectedList = mutableMapOf<UserSelection, Chip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        updateActionBarTitle(R.string.title_add_group_conversation)

        enableSaveButton { submitCreateGroup() }

        toolbarButton.text = getString(R.string.zxing_button_ok)
        toolbarButton.setTextColor(R.color.purple_500)

        fragment = SuggestionFriendListFragment.newInstance(true)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_suggested_friend, fragment)
            .commit()
    }

    override fun binding() {
        mBinding = ActivityAddGroupConversationBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<UserSelectionListViewModel> =
        UserSelectionListViewModel::class.java

    override fun setupObserver() {
        mViewModel.userSuggestionList.observe(this) { suggestionList ->
            val userSelectionList = suggestionList.stream()
                .filter { it.isSelected }
                .collect(Collectors.toList())

            val userUnSelectionList = mutableListOf<UserSelection>().apply {
                addAll(suggestionList)
                removeAll(userSelectionList)
            }

            userSelectionList.forEachIndexed { index, item ->
                if (!mSelectedList.contains(item)) {
                    val userChip = createUserChip(item) {
                        mViewModel.notifyUserSelect(item)
                        makeButtonEnable(userSelectionList.size > PARTICIPANTS_THRESHOLD)
                    }
                    when (index % 2) {
                        0 -> mBinding.chipUserGroup.addView(userChip)
                        else -> mBinding.chipUserGroup2.addView(userChip)
                    }
                    mSelectedList[item] = userChip
                }
            }

            userUnSelectionList.forEach { us ->
                if (mSelectedList.contains(us)) {
                    mSelectedList[us]?.let { chip -> removeChip(us, chip) }
                }
            }

            makeButtonEnable(userSelectionList.size > PARTICIPANTS_THRESHOLD)
        }
    }

    private fun submitCreateGroup() {
        val selectedItem = mSelectedList.keys
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra(ConversationListFragment.EXTRA_GROUP_PARTICIPANTS, ArrayList(selectedItem))
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun createUserChip(item: UserSelection, onCloseClick: () -> Unit): GlideChip {
        return GlideChip(this).apply {
            text = item.displayName
            setIconUrl(item.photoUrl)
            setOnCloseIconClickListener { chip ->
                removeChip(item, chip)
                onCloseClick.invoke()
            }
        }
    }

    private fun removeChip(item: UserSelection, chip: View) {
        mBinding.chipUserGroup2.removeView(chip)
        mBinding.chipUserGroup.removeView(chip)
        mSelectedList.remove(item)
    }
}