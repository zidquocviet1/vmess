package com.mqv.vmess.reactive

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.network.exception.FirebaseUnauthorizedException
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

object ReactiveExtension {
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

    fun FirebaseUser.authorizeToken(): Single<String> {
        return this.getIdToken(true)
            .toSingle()
            .compose(RxHelper.authorizeUser())
    }

    fun <T : Any> FirebaseUser.authorizedAndGet(caller: (token: String) -> Observable<T>): Observable<T> {
        return this.authorizeToken().flatMapObservable { token -> caller.invoke(token) }
    }

    fun <T : Any> FirebaseUser?.authorizedAndGetWithNullableUser(caller: (token: String) -> Observable<T>): Observable<T> {
        return if (this == null) {
            Observable.error(FirebaseUnauthorizedException(-1))
        } else {
            this.authorizeToken().flatMapObservable { token -> caller.invoke(token) }
        }
    }
}