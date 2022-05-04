package com.mqv.vmess.ui.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mqv.vmess.R
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentColorAndWallpaperBinding
import com.mqv.vmess.ui.data.AlertDialogData
import com.mqv.vmess.ui.data.Type
import com.mqv.vmess.util.NavigationUtil.getNavigationResult

class ColorAndWallpaperFragment :
    BaseFragment<ConversationDetailViewModel, FragmentColorAndWallpaperBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(mBinding) {
            layoutChatColor.setOnClickListener {
                findNavController().navigate(
                    ColorAndWallpaperFragmentDirections.actionColorAndWallpaperBottomSheetDialog(
                        mViewModel.conversationColor.value?.chatColor, null
                    )
                )
            }
            textResetChatColor.setOnClickListener {
                VMessAlertDialogFragment.newInstance(
                    AlertDialogData(
                        R.string.label_reset_chat_color,
                        -1,
                        R.string.action_reset,
                        R.string.action_cancel,
                        Type.LEAVE_GROUP
                    )
                ).setOnPositiveClickListener {
                    mViewModel.resetChatColor()
                }.show(childFragmentManager, null)
            }
            textResetWallpaper.setOnClickListener {
                VMessAlertDialogFragment.newInstance(
                    AlertDialogData(
                        R.string.label_reset_wallpaper,
                        -1,
                        R.string.action_reset,
                        R.string.action_cancel,
                        Type.LEAVE_GROUP
                    )
                ).setOnPositiveClickListener {
                    mViewModel.resetWallpaperColor()
                }.show(childFragmentManager, null)
            }
            layoutWallpaper.setOnClickListener {
                findNavController().navigate(
                    ColorAndWallpaperFragmentDirections.actionColorAndWallpaperBottomSheetDialog(
                        null, mViewModel.conversationColor.value?.wallpaperColor
                    )
                )
            }
        }
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentColorAndWallpaperBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> =
        ConversationDetailViewModel::class.java

    override fun setupObserver() {
        mViewModel.conversationColor.observe(viewLifecycleOwner) { color ->
            color?.let {
                showChatColor(color.chatColor)
                showWallpaperColor(color.wallpaperColor)
            }
        }

        getNavigationResult<String>(
            R.id.colorAndWallpaperFragment,
            KEY_PICK_CHAT_COLOR
        ) { chatColorCode ->
            mViewModel.changeChatColor(chatColorCode)
        }

        getNavigationResult<String>(
            R.id.colorAndWallpaperFragment,
            KEY_PICK_WALLPAPER_COLOR
        ) { wallpaperColorCode ->
            mViewModel.changeWallpaperColor(wallpaperColorCode)
        }
    }

    private fun showChatColor(colorCode: String) {
        mBinding.senderChatBackground.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorCode))
        mBinding.imageColor.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorCode))
    }

    private fun showWallpaperColor(colorCode: String) {
        mBinding.layoutBackground.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorCode))
        mBinding.imageWallpaper.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorCode))
    }

    companion object {
        const val KEY_PICK_CHAT_COLOR = "pick_chat_color"
        const val KEY_PICK_WALLPAPER_COLOR = "pick_wallpaper_color"

        @JvmStatic
        fun newInstance() =
            ColorAndWallpaperFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}