package com.mqv.vmess.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.mqv.vmess.R
import com.mqv.vmess.databinding.DialogImagePreviewBinding
import com.mqv.vmess.ui.permissions.Permission
import com.mqv.vmess.util.MyActivityForResult
import com.mqv.vmess.util.Picture
import com.mqv.vmess.work.DownloadMediaWorkWrapper
import com.mqv.vmess.work.WorkDependency

class PreviewImageDialogFragment : DialogFragment() {
    private val mPermissionLauncher = MyActivityForResult.registerActivityForResult(
        this,
        ActivityResultContracts.RequestMultiplePermissions()
    )
    private var mImageUrl: String? = null
    private var mImageProfile: Boolean = false
    private var mBinding: DialogImagePreviewBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mImageUrl = it.getString(ARG_IMAGE_URL)
            mImageProfile = it.getBoolean(ARG_IMAGE_PROFILE, false)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(
            requireContext(),
            android.R.style.Theme_Material_NoActionBar_TranslucentDecor
        ).apply {
            setContentView(
                DialogImagePreviewBinding.bind(
                    layoutInflater.inflate(
                        R.layout.dialog_image_preview,
                        null
                    )
                ).run {
                    mBinding = this
                    root
                })
        }
    }

    override fun onResume() {
        super.onResume()

        mBinding?.run {
            imgBack.setOnClickListener { dismiss() }
            imgMoreVert.setOnClickListener(::showOptionPopupMenu)

            val context = this@PreviewImageDialogFragment.requireContext()
            if (mImageProfile) {
                imageFullScreen.visibility = View.GONE
                imgAvatar.run {
                    visibility = View.VISIBLE
                    Picture.loadUserAvatar(context, mImageUrl).into(this)
                }
            } else {
                imgAvatar.visibility = View.GONE
                imageFullScreen.run {
                    visibility = View.VISIBLE
                    Picture.loadUserAvatar(context, mImageUrl).centerInside().into(this)
                }
            }
        }
    }

    private fun showOptionPopupMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.menu_preview_image)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_save -> saveImage()
                    R.id.menu_share_image -> shareImage()
                    R.id.menu_open_in_browser -> openImageInBrowser()
                }
                false
            }
        }.show()
    }

    private fun saveImage() {
        mImageUrl?.let {
            Permission.with(
                this,
                mPermissionLauncher
            )
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .ifNecessary(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                .onAllGranted {
                    Toast.makeText(
                        requireContext(),
                        "Downloading...",
                        Toast.LENGTH_SHORT
                    ).show()
                    WorkDependency.enqueue(
                        DownloadMediaWorkWrapper(
                            requireContext(),
                            mImageUrl,
                            false
                        )
                    )
                }
                .withRationaleDialog(
                    getString(R.string.msg_permission_external_storage_rational),
                    R.drawable.ic_round_storage_24
                )
                .withPermanentDenialDialog(
                    getString(R.string.msg_permission_allow_app_use_external_storage_title),
                    getString(R.string.msg_permission_external_storage_message),
                    getString(
                        R.string.msg_permission_settings_construction,
                        getString(R.string.label_storage)
                    )
                )
                .execute()
        }
    }

    private fun shareImage() {
        mImageUrl?.let {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    mImageUrl
                )
            })
        }
    }

    private fun openImageInBrowser() {
        mImageUrl?.let {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                Uri.parse(mImageUrl)
            })
        }
    }

    companion object {
        const val ARG_IMAGE_URL = "image_url"
        const val ARG_IMAGE_PROFILE = "image_profile"

        @JvmStatic
        fun newInstance(imageUrl: String? = null, isImageProfile: Boolean = false) =
            PreviewImageDialogFragment().apply {
                bundleOf(
                    ARG_IMAGE_URL to imageUrl,
                    ARG_IMAGE_PROFILE to isImageProfile
                )
            }
    }
}