package com.mqv.vmess.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.mqv.vmess.activity.AddGroupConversationActivity
import com.mqv.vmess.data.repository.ConversationRepository
import com.mqv.vmess.data.result.Result
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.type.ConversationStatusType
import com.mqv.vmess.reactive.RxHelper.parseResponseData
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.util.MyActivityForResult
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.stream.Collectors

class ConversationOptionHandler(private val mContext: Context) {
    fun addMember(
        launcher: MyActivityForResult<Intent, ActivityResult>,
        conversation: Conversation,
        onMemberSelected: (List<String>) -> Unit
    ) {
        val participantIds = conversation.participants.stream()
            .map { u -> u.uid }
            .collect(Collectors.toCollection { ArrayList<String>() })

        val intent = Intent(mContext, AddGroupConversationActivity::class.java).apply {
            putStringArrayListExtra(EXTRA_GROUP_MEMBER_ID, participantIds)
            putExtra(AddGroupConversationActivity.EXTRA_ADD_MEMBER, true)
        }

        launcher.launch(intent) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getParcelableArrayListExtra<UserSelection>(AddGroupConversationActivity.EXTRA_GROUP_PARTICIPANTS)
                    ?.let { selectedMember ->
                        onMemberSelected(selectedMember.stream().map { it.uid }
                            .collect(Collectors.toList()))
                    }
            }
        }
    }

    fun muteNotification() {

    }

    companion object {
        const val EXTRA_GROUP_MEMBER_ID = "group_member_id"

        @JvmStatic
        fun leaveGroup(
            conversationRepository: ConversationRepository,
            id: String
        ): Observable<Result<Conversation>> {
            val subject = BehaviorSubject.create<Result<Conversation>>()

            conversationRepository.leaveGroup(id)
                .subscribeOn(Schedulers.io())
                .startWith(Completable.fromAction { subject.onNext(Result.Loading()) })
                .compose(parseResponseData())
                .flatMap { c ->
                    subject.onNext(Result.Success(c))
                    subject.onNext(Result.Terminate())
                    conversationRepository.delete(c).toObservable<Result<Conversation>>()
                }
                .subscribe(subject)

            return subject
        }

        @JvmStatic
        fun addMemberObservable(
            conversationRepository: ConversationRepository,
            conversationId: String,
            memberId: String
        ): Observable<Result<Conversation>> {
            val subject = BehaviorSubject.create<Result<Conversation>>()

            conversationRepository.addGroupMember(conversationId, memberId)
                .subscribeOn(Schedulers.io())
                .startWith(Completable.fromAction { subject.onNext(Result.Loading()) })
                .compose(parseResponseData())
                .flatMap { c ->
                    c.status = ConversationStatusType.INBOX
                    subject.onNext(Result.Success(c))
                    conversationRepository.save(c).andThen {
                        Completable.fromAction {
                            AppDependencies.getDatabaseObserver()
                                .notifyMessageInserted(c.id, c.chats[0].id)
                        }
                    }.toObservable<Result<Conversation>>()
                }
                .subscribe(subject)

            return subject
        }
    }
}