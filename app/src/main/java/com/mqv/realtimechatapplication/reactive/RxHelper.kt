package com.mqv.realtimechatapplication.reactive

import com.google.firebase.auth.GetTokenResult
import com.mqv.realtimechatapplication.network.ApiResponse
import com.mqv.realtimechatapplication.network.exception.BadRequestException
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException
import com.mqv.realtimechatapplication.network.exception.ResourceConflictException
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException
import com.mqv.realtimechatapplication.util.Const
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.schedulers.Schedulers

object RxHelper {
    @JvmStatic
    fun <T : Any> applyObservableSchedulers(): ObservableTransformer<T, T> =
        ObservableTransformer {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

    @JvmStatic
    fun <T : Any> applyFlowableSchedulers(): FlowableTransformer<T, T> =
        FlowableTransformer {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

    @JvmStatic
    fun <T : Any> applySingleSchedulers(): SingleTransformer<T, T> =
        SingleTransformer {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

    @JvmStatic
    fun <T : Any> applyMaybeSchedulers(): MaybeTransformer<T, T> =
        MaybeTransformer {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

    @JvmStatic
    fun applyCompleteSchedulers(): CompletableTransformer =
        CompletableTransformer {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

    fun authorizeUser(): SingleTransformer<in GetTokenResult, String> =
        SingleTransformer { single ->
            single.flatMap { result ->
                return@flatMap if (result.token == null) {
                    Single.error(FirebaseUnauthorizedException(-1))
                } else {
                    Single.just("${Const.PREFIX_TOKEN}${result.token!!}")
                }
            }
        }

    @JvmStatic
    fun <T : Any> parseResponseData(): ObservableTransformer<ApiResponse<T>, T> {
        return ObservableTransformer { observable ->
            observable.flatMap { response ->
                if (response.statusCode in 200..299) {
                    return@flatMap createData(response.success)
                } else {
                    return@flatMap handleError(response.statusCode)
                }
            }
        }
    }

    @JvmStatic
    fun <T : Any> parseSingleResponseData(): SingleTransformer<ApiResponse<T>, T> {
        return SingleTransformer { single ->
            single.flatMap { response ->
                if (response.statusCode in 200..299) {
                    return@flatMap createSingleData(response.success)
                } else {
                    return@flatMap handleSingleError(response.statusCode)
                }
            }
        }
    }

    private fun <T : Any> createSingleData(data: T): Single<T> =
        Single.create { emitter ->
            try {
                emitter.onSuccess(data)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }

    private fun <T : Any> createData(data: T): Observable<T> =
        Observable.create { emitter ->
            try {
                emitter.onNext(data)
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }

    private fun <T : Any> handleError(statusCode: Int): Observable<T> {
        return Observable.error(parseError(statusCode))
    }

    private fun <T : Any> handleSingleError(statusCode: Int): Single<T> {
        return Single.error(parseError(statusCode))
    }

    private fun parseError(statusCode: Int): Throwable {
        return when (statusCode) {
            403 -> BadRequestException()
            404 -> ResourceNotFoundException()
            409 -> ResourceConflictException()
            else -> IllegalStateException()
        }
    }
}