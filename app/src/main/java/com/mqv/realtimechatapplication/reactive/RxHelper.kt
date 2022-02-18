package com.mqv.realtimechatapplication.reactive

import com.mqv.realtimechatapplication.network.ApiResponse
import com.mqv.realtimechatapplication.network.exception.BadRequestException
import com.mqv.realtimechatapplication.network.exception.ResourceConflictException
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException
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
        return when (statusCode) {
            403 -> Observable.error(BadRequestException())
            404 -> Observable.error(ResourceNotFoundException())
            409 -> Observable.error(ResourceConflictException())
            else -> Observable.error(IllegalStateException())
        }
    }
}