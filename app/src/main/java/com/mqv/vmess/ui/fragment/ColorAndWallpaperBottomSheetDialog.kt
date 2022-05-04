package com.mqv.vmess.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mqv.vmess.R
import com.mqv.vmess.databinding.DialogBottomColorWallpaperPickerBinding
import com.mqv.vmess.ui.adapter.BaseAdapter
import com.mqv.vmess.ui.adapter.ColorPickerAdapter
import com.mqv.vmess.ui.data.ColorPicker
import com.mqv.vmess.util.NavigationUtil.setNavigationResult

private const val ARG_CURRENT_CHAT_COLOR = "current_color"
private const val ARG_CURRENT_WALL_PAPER_COLOR = "current_wallpaper_color"

class ColorAndWallpaperBottomSheetDialog : BottomSheetDialogFragment() {
    private lateinit var mBinding: DialogBottomColorWallpaperPickerBinding
    private lateinit var mCurrentChatColor: String
    private lateinit var mCurrentWallpaperColor: String
    private lateinit var mAdapter: ColorPickerAdapter

    private var mSelectedIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mCurrentChatColor = it.getString(ARG_CURRENT_CHAT_COLOR) ?: ""
            mCurrentWallpaperColor = it.getString(ARG_CURRENT_WALL_PAPER_COLOR) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DialogBottomColorWallpaperPickerBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        COLORS.stream()
            .forEach { cp ->
                mSelectedIndex = COLORS.indexOf(cp)
                cp.isSelected = (cp.colorCode == mCurrentChatColor) || (cp.colorCode == mCurrentWallpaperColor)
            }

        with(mBinding) {
            mAdapter = ColorPickerAdapter()
            mAdapter.submitList(COLORS)
            list.adapter = mAdapter
            list.layoutManager =
                GridLayoutManager(requireContext(), 4, GridLayoutManager.VERTICAL, false)
            list.setHasFixedSize(true)

            mAdapter.registerEventHandler(object : BaseAdapter.ItemEventHandler {
                override fun onItemClick(position: Int) {
                    mAdapter.currentList.apply {
                        this[position].isSelected = !this[position].isSelected
                        if (mSelectedIndex != -1) {
                            this[mSelectedIndex].isSelected = false
                        }

                        mAdapter.notifyItemChanged(
                            mSelectedIndex,
                            ColorPickerAdapter.PAYLOAD_SELECT
                        )

                        mSelectedIndex = position
                    }
                    mAdapter.notifyItemChanged(mSelectedIndex, ColorPickerAdapter.PAYLOAD_SELECT)
                    if (mCurrentChatColor.isNotEmpty()) {
                        setNavigationResult(
                            ColorAndWallpaperFragment.KEY_PICK_CHAT_COLOR,
                            mAdapter.currentList[mSelectedIndex].colorCode
                        )
                    } else {
                        setNavigationResult(
                            ColorAndWallpaperFragment.KEY_PICK_WALLPAPER_COLOR,
                            mAdapter.currentList[mSelectedIndex].colorCode
                        )
                    }
                    dismiss()
                }
            })
        }
    }

    override fun getTheme(): Int = R.style.UserSocialLinkListDialogFragment

    companion object {
        fun newInstance(
            currentChatColor: String,
            currentWallpaperColor: String
        ) =
            ColorAndWallpaperBottomSheetDialog().apply {
                arguments = bundleOf(
                    ARG_CURRENT_CHAT_COLOR to currentChatColor,
                    ARG_CURRENT_WALL_PAPER_COLOR to currentWallpaperColor
                )
            }

        val COLORS = mutableListOf(
            ColorPicker(colorCode = "#FF6200EE", isDefault = true),
            ColorPicker(colorCode = "#FFBB86FC"),
            ColorPicker(colorCode = "#FF3700B3"),
            ColorPicker(colorCode = "#FF03DAC5"),
            ColorPicker(colorCode = "#FF018786"),
            ColorPicker(colorCode = "#FFFF5F6D"),
            ColorPicker(colorCode = "#FFEA8D8D"),
            ColorPicker(colorCode = "#FFA890FE"),
            ColorPicker(colorCode = "#FFFE90AF"),
            ColorPicker(colorCode = "#FFC6EA8D"),
            ColorPicker(colorCode = "#FFFF61D2"),
            ColorPicker(colorCode = "#FFFE9090"),
        )
    }
}