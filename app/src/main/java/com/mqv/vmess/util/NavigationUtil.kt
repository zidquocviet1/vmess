package com.mqv.vmess.util

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController

object NavigationUtil {
    /*
    * Find two top extension function on:
    * https://stackoverflow.com/questions/56624895/android-jetpack-navigation-component-result-from-dialog
    * */
    fun <T> Fragment.setNavigationResult(key: String, value: T) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(
            key,
            value
        )
    }

    fun <T> Fragment.getNavigationResult(
        @IdRes id: Int,
        key: String,
        onResult: (result: T) -> Unit
    ) {
        try {
            val navBackStackEntry = findNavController().getBackStackEntry(id)

            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && navBackStackEntry.savedStateHandle.contains(
                        key
                    )
                ) {
                    val result = navBackStackEntry.savedStateHandle.get<T>(key)
                    result?.let(onResult)
                    navBackStackEntry.savedStateHandle.remove<T>(key)
                }
            }
            navBackStackEntry.lifecycle.addObserver(observer)

            viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    navBackStackEntry.lifecycle.removeObserver(observer)
                }
            })
        } catch (e: IllegalArgumentException) {
        }
    }
}