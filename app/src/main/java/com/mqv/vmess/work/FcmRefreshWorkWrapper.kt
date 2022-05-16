package com.mqv.vmess.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.work.rxjava3.RxWorker
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.network.firebase.FcmUtil
import com.mqv.vmess.network.service.UserService
import com.mqv.vmess.reactive.ReactiveExtension.authorizeToken
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.util.Const
import com.mqv.vmess.util.Logging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Single

class FcmRefreshWorkWrapper(context: Context) : BaseWorker(context) {
    override fun retrieveConstraint(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun isUniqueWork(): Boolean = false

    override fun createRequest(): WorkRequest =
        OneTimeWorkRequest.Builder(FcmRefreshWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(retrieveConstraint())
            .build()


    @HiltWorker
    class FcmRefreshWorker @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted parameter: WorkerParameters,
        private val userService: UserService
    ) : RxWorker(context, parameter) {
        override fun createWork(): Single<Result> {
            return Single.create<String> { emitter ->
                if (!emitter.isDisposed) {
                    FcmUtil.getToken().run {
                        if (isPresent) {
                            get()
                        } else {
                            Single.error<String>(RuntimeException())
                        }
                    }
                } else {
                    Single.error<String>(RuntimeException())
                }
            }
                .doOnError {
                    Logging.debug(
                        TAG,
                        "The fcm token is empty, no need to register to the server."
                    )
                }
                .flatMap { token -> pushFcmToken(token) }
                .onErrorReturnItem(Result.failure())
        }

        private fun pushFcmToken(fcmToken: String): Single<Result> {
            val mUser = FirebaseAuth.getInstance().currentUser

            return if (mUser == null) {
                Logging.debug(TAG, "The current user is not logged in. So FcmRefreshJob failed")

                Single.just(Result.failure())
            } else {
                mUser.authorizeToken()
                    .flatMapObservable { token ->
                        userService.sendFcmTokenToServer(
                            token,
                            Const.AUTHORIZER,
                            fcmToken
                        )
                    }
                    .compose(RxHelper.applyObservableSchedulers())
                    .compose(RxHelper.parseResponseData())
                    .singleOrError()
                    .map { Result.success() }
                    .onErrorReturnItem(Result.failure())
            }
        }

        companion object {
            private val TAG: String = FcmRefreshWorker::class.java.simpleName
        }
    }
}