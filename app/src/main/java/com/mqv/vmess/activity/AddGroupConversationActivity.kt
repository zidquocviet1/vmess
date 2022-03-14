package com.mqv.vmess.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.chip.Chip
import com.mqv.vmess.R
import com.mqv.vmess.activity.viewmodel.UserSelectionListViewModel
import com.mqv.vmess.databinding.ActivityAddGroupConversationBinding
import com.mqv.vmess.ui.GlideChip
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.ui.fragment.SuggestionFriendListFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.stream.Collectors

@AndroidEntryPoint
class AddGroupConversationActivity :
    ToolbarActivity<UserSelectionListViewModel, ActivityAddGroupConversationBinding>() {

    private lateinit var fragment: SuggestionFriendListFragment
    private val mSelectedList = mutableMapOf<UserSelection, Chip>()
    private var participantsThreshold = 2 // Minimum member to create new group

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

        participantsThreshold = if (intent.getBooleanExtra(EXTRA_ADD_MEMBER, false)) 1 else 2
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
                        makeButtonEnable(userSelectionList.size >= participantsThreshold)
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

            makeButtonEnable(userSelectionList.size >= participantsThreshold)
        }
    }

    private fun submitCreateGroup() {
        val selectedItem = mSelectedList.keys
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra(EXTRA_GROUP_PARTICIPANTS, ArrayList(selectedItem))
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

    companion object {
        const val EXTRA_GROUP_PARTICIPANTS = "group_participants"
        const val EXTRA_ADD_MEMBER = "add_member"
    }
}