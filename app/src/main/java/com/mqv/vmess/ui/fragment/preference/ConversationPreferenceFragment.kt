package com.mqv.vmess.ui.fragment.preference

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mqv.vmess.R
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.ui.components.ConversationPreference
import com.mqv.vmess.ui.data.ConversationDetail
import com.mqv.vmess.ui.fragment.ConversationDetailFragment.Companion.KEY_MUTE_NOTIFICATION
import com.mqv.vmess.util.DateTimeHelper.toLong
import java.time.LocalDateTime

class ConversationPreferenceFragment : PreferenceFragmentCompat() {
    interface ConversationPreferenceListener {
        fun onChatAndWallPaper()
        fun onDisappearingMessages()
        fun onNotificationAndSound()
        fun onViewMediaAndFile()
        fun onCreateGroup(user: User)
        fun onSeeGroupMember()
        fun onIgnoreMessage()
        fun onBlock()
        fun onReport()
    }

    private var mCallback: ConversationPreferenceListener? = null
    private lateinit var mConversationDetail: ConversationDetail

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ConversationPreferenceListener) {
            mCallback = context
        } else {
            throw IllegalStateException("Context must implement ConversationPreferenceListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mConversationDetail = it.getParcelable(ARG_CONVERSATION)!!
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_conversation_fragment, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        findPreference<Preference>(getString(R.string.key_pref_chat_wallpaper))?.let { preference ->
            (preference as ConversationPreference).apply {
                setTitle(R.string.title_chat_color_and_wallpaper)
                setIcon(R.drawable.ic_round_wallpaper)
            }
        }

        findPreference<Preference>(getString(R.string.key_pref_disappearing_messages))?.let { preference ->
            (preference as ConversationPreference).apply {
                setTitle(R.string.title_disappearing_messages)
                setIcon(R.drawable.ic_round_access_alarm)
            }
        }

        findPreference<Preference>(getString(R.string.key_pref_notifications_and_sounds))?.let { preference ->
            (preference as ConversationPreference).apply {
                if (mConversationDetail.notificationOption.until < LocalDateTime.now().toLong()) {
                    setSummary(R.string.label_on)
                } else {
                    setSummary(R.string.label_off)
                }
                setTitle(R.string.title_preference_item_notification_and_sounds)
                setIcon(R.drawable.ic_round_notifications)
            }
        }

        findPreference<Preference>(getString(R.string.key_pref_view_media_files))?.let { preference ->
            (preference as ConversationPreference).apply {
                setTitle(R.string.title_view_media_and_files)
                setIcon(R.drawable.ic_round_photo_library)
            }
        }
        findPreference<Preference>(getString(R.string.key_pref_create_group_with))?.let { preference ->
            (preference as ConversationPreference).apply {
                val participants = mConversationDetail.metadata.conversationParticipants
                val other = participants.filter { user -> user.uid == mConversationDetail.metadata.otherUid }[0]

                setTitle(getString(R.string.label_conversation_create_group, other.displayName))
                setIcon(R.drawable.ic_round_groups)
                isVisible = mConversationDetail.metadata.type != ConversationType.GROUP
            }
        }

        findPreference<Preference>(getString(R.string.key_pref_see_group_member))?.let { preference ->
            (preference as ConversationPreference).apply {
                setTitle(R.string.label_conversation_pref_see_group_members)
                setIcon(R.drawable.ic_round_groups)

                isVisible = mConversationDetail.metadata.type == ConversationType.GROUP
            }
        }
        findPreference<Preference>(getString(R.string.key_pref_ignore_messages))?.let { preference ->
            (preference as ConversationPreference).apply {
                setTitle(R.string.label_conversation_ignore_messages)
                setIcon(R.drawable.ic_round_public_off)
            }
        }
        findPreference<Preference>(getString(R.string.key_pref_block))?.let { preference ->
            (preference as ConversationPreference).apply {
                setTitle(R.string.title_pref_block)
                setIcon(R.drawable.ic_round_remove_circle)
            }
        }
        findPreference<Preference>(getString(R.string.key_pref_report))?.let { preference ->
            (preference as ConversationPreference).apply {
                setTitle(R.string.title_pref_report_problem)
                setIcon(R.drawable.ic_round_reply)
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.apply {
            isNestedScrollingEnabled = false
            isVerticalScrollBarEnabled = false
            setHasFixedSize(false)
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
            KEY_MUTE_NOTIFICATION
        )?.observe(viewLifecycleOwner) { isMute ->
            findPreference<Preference>(getString(R.string.key_pref_notifications_and_sounds))?.let { preference ->
                (preference as ConversationPreference).apply {
                    setSummaryAndNotify(if (isMute) R.string.label_off else R.string.label_on)
                }
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.key_pref_chat_wallpaper) -> mCallback?.onChatAndWallPaper()
            getString(R.string.key_pref_disappearing_messages) -> mCallback?.onDisappearingMessages()
            getString(R.string.key_pref_notifications_and_sounds) -> mCallback?.onNotificationAndSound()
            getString(R.string.key_pref_view_media_files) -> mCallback?.onViewMediaAndFile()
            getString(R.string.key_pref_see_group_member) -> mCallback?.onSeeGroupMember()
            getString(R.string.key_pref_create_group_with) -> {
                val metadata = mConversationDetail.metadata
                val participants = metadata.conversationParticipants

                mCallback?.onCreateGroup(participants.filter { user -> user.uid != metadata.currentUserId }[0])
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    companion object {
        const val ARG_CONVERSATION = "conversation"

        fun newInstance(conversationDetail: ConversationDetail): ConversationPreferenceFragment =
            ConversationPreferenceFragment().apply {
                arguments = bundleOf(ARG_CONVERSATION to conversationDetail)
            }
    }
}