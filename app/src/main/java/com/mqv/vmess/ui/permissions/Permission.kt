package com.mqv.vmess.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mqv.vmess.R
import com.mqv.vmess.util.MyActivityForResult
import java.lang.ref.WeakReference
import java.security.SecureRandom
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.streams.toList

class Permission {
    class LRUCache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > maxSize
    }

    companion object {
        val OUTSTANDING = LRUCache<Int, PermissionRequest>(2)

        @JvmStatic
        fun with(
            activity: Activity,
            permissionLauncher: MyActivityForResult<Array<String>, Map<String, Boolean>>
        ) = PermissionBuilder(ActivityPermissionLauncherObject(activity, permissionLauncher))

        @JvmStatic
        fun with(
            fragment: Fragment,
            permissionLauncher: MyActivityForResult<Array<String>, Map<String, Boolean>>
        ) = PermissionBuilder(FragmentPermissionLauncherObject(fragment, permissionLauncher))

        fun hasAll(context: Context, vararg permissions: String) =
            Stream.of(*permissions).allMatch { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }

        fun hasAny(context: Context, vararg permissions: String) =
            Stream.of(*permissions).anyMatch { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }

        fun requestPermissions(
            caller: PermissionObject,
            context: Context,
            requestCode: Int,
            permissions: Array<String>,
            launcher: MyActivityForResult<Array<String>, Map<String, Boolean>>
        ) {
            val neededPermissions = filterNotGranted(context, permissions)

            if (neededPermissions.isEmpty()) return

            launcher.launch(permissions) { map ->
                onRequestPermissionsResult(caller, requestCode, map)
            }
        }

        fun getApplicationSettingsIntent(context: Context) =
            Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
            }

        fun getWindowWidth(context: Context): Int {
            val windowManager =
                ContextCompat.getSystemService(context, WindowManager::class.java)!!

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                windowMetrics.bounds.width() - insets.left - insets.right
            } else {
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics.widthPixels
            }
        }

        private fun filterNotGranted(context: Context, permissions: Array<String>): Array<String> =
            Stream.of(*permissions)
                .filter { permission ->
                    ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == PackageManager.PERMISSION_DENIED
                }
                .toList()
                .toTypedArray()

        private fun onRequestPermissionsResult(
            caller: PermissionObject,
            requestCode: Int,
            result: Map<String, Boolean>
        ) {
            var permissionRequest: PermissionRequest?

            synchronized(OUTSTANDING) {
                permissionRequest = OUTSTANDING.remove(requestCode)
            }

            permissionRequest?.let {
                val shouldShowRationaleDialog = BooleanArray(result.size)
                var index = 0

                for (entry in result) {
                    if (!entry.value) {
                        shouldShowRationaleDialog[index] =
                            caller.shouldShowPermissionRationale(entry.key)
                        index += 1
                    }
                }

                val grantResults =
                    result.values.map { isGranted -> if (isGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED }
                        .toList()
                        .toIntArray()

                it.onResult(result.keys.toTypedArray(), grantResults, shouldShowRationaleDialog)
            }
        }

        class FragmentPermissionLauncherObject(
            private val fragment: Fragment,
            private val launcher: MyActivityForResult<Array<String>, Map<String, Boolean>>
        ) : PermissionObject() {
            override fun getContext() = fragment.requireContext()

            override fun shouldShowPermissionRationale(permissionName: String) =
                fragment.shouldShowRequestPermissionRationale(permissionName)

            override fun hasAll(permissions: Array<String>) =
                Companion.hasAll(getContext(), *permissions)

            override fun requestPermissions(requestCode: Int, permissions: Array<String>) {
                Companion.requestPermissions(this, getContext(), requestCode, permissions, launcher)
            }
        }

        class ActivityPermissionLauncherObject(
            private val activity: Activity,
            private val launcher: MyActivityForResult<Array<String>, Map<String, Boolean>>
        ) : PermissionObject() {

            override fun getContext() = activity

            override fun shouldShowPermissionRationale(permissionName: String) =
                activity.shouldShowRequestPermissionRationale(permissionName)

            override fun hasAll(permissions: Array<String>) =
                Companion.hasAll(getContext(), *permissions)

            override fun requestPermissions(requestCode: Int, permissions: Array<String>) {
                Companion.requestPermissions(this, getContext(), requestCode, permissions, launcher)
            }
        }

        class SettingsDialogRunnable(
            context: Context,
            private val title: String,
            private val message: String,
            private val constructionMessage: String
        ) : Runnable {
            private var context: WeakReference<Context> = WeakReference(context)

            override fun run() {
                this.context.get()?.let {
                    RationaleDialog.createForPermanentlyDenied(
                        it,
                        title,
                        message,
                        constructionMessage
                    )
                        .setPositiveButton(R.string.label_settings) { _, _ ->
                            it.startActivity(
                                getApplicationSettingsIntent(it)
                            )
                        }
                        .setNegativeButton(R.string.action_cancel, null)
                        .show()
                        .window
                        ?.setLayout(
                            (getWindowWidth(it) * 0.75).toInt(),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                }
            }
        }
    }

    class PermissionBuilder(
        private val permissionObject: PermissionObject,
        private var requestedPermissions: Array<String>? = null,
        private var allGrantedListener: Runnable? = null,
        private var anyDeniedListener: Runnable? = null,
        private var anyPermanentlyDeniedListener: Runnable? = null,
        private var anyResultListener: Runnable? = null,
        private var someGrantedListener: Consumer<List<String>>? = null,
        private var someDeniedListener: Consumer<List<String>>? = null,
        private var somePermanentlyDeniedListener: Consumer<List<String>>? = null,
        private var rationaleDialogHeaderIcon: IntArray? = null,
        private var rationaleDialogMessage: String? = null,
        private var rationaleDialogCancelable: Boolean = false,
        private var ifNecessary: Boolean = false,
        private var condition: Boolean = true
    ) {
        fun request(vararg permissions: String): PermissionBuilder {
            this.requestedPermissions = arrayOf(*permissions)
            return this
        }

        fun ifNecessary(): PermissionBuilder {
            this.ifNecessary = true
            return this
        }

        fun ifNecessary(condition: Boolean): PermissionBuilder {
            this.ifNecessary = true
            this.condition = condition
            return this
        }

        fun withRationaleDialog(
            message: String,
            @DrawableRes vararg headerIcon: Int
        ): PermissionBuilder {
            return withRationaleDialog(
                message = message,
                cancelable = true,
                headerIcon = headerIcon
            )
        }

        fun withRationaleDialog(
            message: String,
            cancelable: Boolean,
            @DrawableRes vararg headerIcon: Int
        ): PermissionBuilder {
            this.rationaleDialogMessage = message
            this.rationaleDialogCancelable = cancelable
            this.rationaleDialogHeaderIcon = intArrayOf(*headerIcon)
            return this
        }

        fun withPermanentDenialDialog(title: String, message: String, constructionMessage: String) =
            onAnyPermanentlyDenied(
                SettingsDialogRunnable(
                    permissionObject.getContext(),
                    title,
                    message,
                    constructionMessage
                )
            )

        fun onAllGranted(allGrantedListener: Runnable): PermissionBuilder {
            this.allGrantedListener = allGrantedListener
            return this
        }

        fun onAnyDenied(anyDeniedListener: Runnable): PermissionBuilder {
            this.anyDeniedListener = anyDeniedListener
            return this
        }

        fun onAnyPermanentlyDenied(anyPermanentlyDeniedListener: Runnable): PermissionBuilder {
            this.anyPermanentlyDeniedListener = anyPermanentlyDeniedListener
            return this
        }

        fun onAnyResult(anyResultListener: Runnable): PermissionBuilder {
            this.anyResultListener = anyResultListener
            return this
        }

        fun onSomeGranted(someGrantedListener: Consumer<List<String>>): PermissionBuilder {
            this.someGrantedListener = someGrantedListener
            return this
        }

        fun onSomeDenied(someDeniedListener: Consumer<List<String>>): PermissionBuilder {
            this.someDeniedListener = someDeniedListener
            return this
        }

        fun onSomePermanentlyDenied(somePermanentlyDeniedListener: Consumer<List<String>>): PermissionBuilder {
            this.somePermanentlyDeniedListener = somePermanentlyDeniedListener
            return this
        }

        fun execute() {
            val permissionRequest = PermissionRequest(
                allGrantedListener,
                anyDeniedListener,
                anyPermanentlyDeniedListener,
                anyResultListener,
                someGrantedListener,
                someDeniedListener,
                somePermanentlyDeniedListener
            )

            if (ifNecessary && (permissionObject.hasAll(requestedPermissions!!) || !condition)) {
                executePreGrantedPermissionsRequest(permissionRequest)
            } else if (rationaleDialogMessage != null && rationaleDialogHeaderIcon != null) {
                executePermissionsRequestWithRationale(permissionRequest)
            } else {
                executePermissionsRequest(permissionRequest)
            }
        }

        private fun executePreGrantedPermissionsRequest(request: PermissionRequest) {
            val grantResults = Stream.of(requestedPermissions)
                .map { PackageManager.PERMISSION_GRANTED }
                .toList()
                .toIntArray()

            request.onResult(
                requestedPermissions!!,
                grantResults,
                BooleanArray(requestedPermissions!!.size) { false })
        }

        private fun executePermissionsRequestWithRationale(request: PermissionRequest) {
            RationaleDialog.createFor(
                permissionObject.getContext(),
                rationaleDialogMessage!!,
                rationaleDialogHeaderIcon!!
            )
                .setPositiveButton(R.string.action_continue) { _, _ ->
                    executePermissionsRequest(
                        request
                    )
                }
                .setNegativeButton(R.string.action_not_now) { _, _ ->
                    executeNoPermissionsRequest(
                        request
                    )
                }
                .setCancelable(rationaleDialogCancelable)
                .show()
                .window
                ?.setLayout(
                    (permissionObject.getWindowWidth() * .75).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
        }

        private fun executePermissionsRequest(request: PermissionRequest) {
            val requestCode = SecureRandom().nextInt(63333) + 100

            synchronized(OUTSTANDING) {
                OUTSTANDING[requestCode] = request
            }

            for (permission in requestedPermissions!!) {
                request.addMapping(
                    permission,
                    permissionObject.shouldShowPermissionRationale(permission)
                )
            }

            permissionObject.requestPermissions(requestCode, requestedPermissions!!)
        }

        private fun executeNoPermissionsRequest(request: PermissionRequest) {
            for (permission in requestedPermissions!!) {
                request.addMapping(permission, true)
            }

            val neededPermissions =
                filterNotGranted(permissionObject.getContext(), requestedPermissions!!)
            val notGrantResults =
                Stream.of(neededPermissions).mapToInt { PackageManager.PERMISSION_DENIED }
                    .toList().toIntArray()
            val showDialog = BooleanArray(neededPermissions.size) { true }

            request.onResult(neededPermissions, notGrantResults, showDialog)
        }
    }

    abstract class PermissionObject {
        abstract fun getContext(): Context
        abstract fun shouldShowPermissionRationale(permissionName: String): Boolean
        abstract fun hasAll(permissions: Array<String>): Boolean
        abstract fun requestPermissions(requestCode: Int, permissions: Array<String>)

        fun getWindowWidth(): Int {
            val windowManager =
                ContextCompat.getSystemService(getContext(), WindowManager::class.java)!!

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                windowMetrics.bounds.width() - insets.left - insets.right
            } else {
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics.widthPixels
            }
        }
    }
}