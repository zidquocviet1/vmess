package com.mqv.vmess.ui.fragment

import android.Manifest
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mqv.vmess.R
import com.mqv.vmess.ui.permissions.Permission.Companion.with
import com.mqv.vmess.util.FileProviderUtil.createTempFilePicture
import com.mqv.vmess.util.MyActivityForResult
import com.mqv.vmess.util.NavigationUtil.setNavigationResult

class HandleThumbnailDialogFragment : DialogFragment() {
    private val mPermissionLauncher = MyActivityForResult.registerActivityForResult(
        this,
        ActivityResultContracts.RequestMultiplePermissions()
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setItems(R.array.action_change_avatar) { _, which ->
                if (which == 0) {
                    with(this, mPermissionLauncher)
                        .request(Manifest.permission.CAMERA)
                        .ifNecessary()
                        .onAllGranted { this.handleTakePicture() }
                        .withRationaleDialog(
                            getString(R.string.msg_permission_camera_rational),
                            R.drawable.ic_camera
                        )
                        .withPermanentDenialDialog(
                            getString(R.string.msg_permission_allow_app_use_camera_title),
                            getString(R.string.msg_permission_camera_message),
                            getString(
                                R.string.msg_permission_settings_construction,
                                getString(R.string.label_camera)
                            )
                        )
                        .execute()
                } else {
                    with(this, mPermissionLauncher)
                        .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .ifNecessary(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                        .onAllGranted { setNavigationResult(ConversationDetailFragment.KEY_CHOOSE_PHOTO, true) }
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
            .create()
    }

    private fun handleTakePicture() {
        val uri = createTempFilePicture(requireContext().contentResolver)

        setNavigationResult(ConversationDetailFragment.KEY_TAKE_PHOTO, uri)
    }
}