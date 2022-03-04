package com.mqv.realtimechatapplication.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.work.rxjava3.RxWorker
import com.google.firebase.auth.FirebaseAuth
import com.mqv.realtimechatapplication.data.dao.ConversationDao
import com.mqv.realtimechatapplication.dependencies.AppDependencies
import com.mqv.realtimechatapplication.network.model.Conversation
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType
import com.mqv.realtimechatapplication.network.service.ConversationService
import com.mqv.realtimechatapplication.reactive.ReactiveExtension.authorizeToken
import com.mqv.realtimechatapplication.reactive.RxHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class FetchConversationWorkWrapper(val mContext: Context, val data: Data) : BaseWorker(mContext) {
    override fun createRequest() = OneTimeWorkRequest.Builder(FetchConversationWorker::class.java)
        .setConstraints(retrieveConstraint())
        .setInputData(data)
        .build()

    override fun retrieveConstraint() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun isUniqueWork() = false

    @HiltWorker
    class FetchConversationWorker @AssistedInject constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val service: ConversationService,
        private val dao: ConversationDao
    ) : RxWorker(appContext, workerParams) {
        private val user = FirebaseAuth.getInstance().currentUser

        override fun createWork(): Single<Result> =
            if (user == null)
                Single.just(Result.failure())
            else
                fetchConversation(inputData)

        private fun fetchConversation(data: Data): Single<Result> {
            val conversationId = data.getString(EXTRA_CONVERSATION_ID)
            val userId = data.getString(EXTRA_USER_ID)

            return when {
                conversationId != null -> fetchByConversationId(conversationId)
                userId != null -> fetchByUserId(userId)
                else -> Single.just(Result.failure())
            }
        }

        private fun fetchByConversationId(conversationId: String): Single<Result> {
            return user!!.authorizeToken()
                .flatMap { service.findById(it, conversationId) }
                .compose(RxHelper.parseSingleResponseData())
                .flatMap { onResponse(it) }
                .onErrorReturnItem(Result.failure())
        }

        private fun fetchByUserId(userId: String): Single<Result> {
            return user!!.authorizeToken()
                .flatMapObservable { service.findNormalByParticipantId(it, userId) }
                .compose(RxHelper.parseResponseData())
                .singleOrError()
                .flatMap { onResponse(it) }
                .onErrorReturnItem(Result.failure())
        }

        private fun onResponse(conversation: Conversation): Single<Result> {
            saveAndNotify(conversation)

            val output = Data.Builder()
                .putString(EXTRA_CONVERSATION_ID, conversation.id)
                .build()

            return Single.just(Result.success(output))
        }

        private fun saveAndNotify(conversation: Conversation) {
            conversation.status = ConversationStatusType.INBOX

            Completable.fromAction { dao.saveConversationList(listOf(conversation)) }
                .compose(RxHelper.applyCompleteSchedulers())
                .andThen {
                    AppDependencies.getDatabaseObserver()
                        .notifyConversationInserted(conversation.id)
                    it.onComplete()
                }
                .onErrorComplete()
                .subscribe()
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_ID = "user_id"
    }
}