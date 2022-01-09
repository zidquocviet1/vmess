package com.mqv.realtimechatapplication.reactive

import com.google.android.gms.tasks.Task
import io.reactivex.rxjava3.core.Single

class ReactiveExtension {
    fun <T : Any> Task<T>.toSingle(): Single<T> {
        return Single.create { emitter ->
            addOnCompleteListener { task ->
                if (task.isSuccessful && !emitter.isDisposed) {
                    emitter.onSuccess(task.result!!)
                } else if (!emitter.isDisposed) {
                    emitter.onError(task.exception!!)
                }
            }
        }
    }
}