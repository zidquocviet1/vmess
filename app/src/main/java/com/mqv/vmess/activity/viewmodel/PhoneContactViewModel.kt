package com.mqv.vmess.activity.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mqv.vmess.R
import com.mqv.vmess.data.repository.FriendRequestRepository
import com.mqv.vmess.data.repository.UserRepository
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.data.PhoneContact
import com.mqv.vmess.ui.fragment.PhoneContactFragment
import com.mqv.vmess.util.Logging
import com.mqv.vmess.data.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

@HiltViewModel
class PhoneContactViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val friendRequestRepository: FriendRequestRepository
) : ViewModel() {
    private val cd = CompositeDisposable()
    private val _phoneContacts = MutableLiveData<Result<List<PhoneContact>>>()
    private val _friendRequestResult = MutableLiveData<Map<Int, Result<PhoneContact>>>()

    val phoneContacts: LiveData<Result<List<PhoneContact>>>
        get() = _phoneContacts
    val friendRequestResult: LiveData<Map<Int, Result<PhoneContact>>>
        get() = _friendRequestResult

    @SuppressLint("Range")
    fun getPhoneContactList(context: Context) {
        val cr = context.contentResolver
        val cur = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )

        cur?.use {
            val result = mutableListOf<Pair<String, String>>()

            while (cur.moveToNext()) {
                val id: String = cur.getString(
                    cur.getColumnIndex(ContactsContract.Contacts._ID)
                )
                val name: String = cur.getString(
                    cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME
                    )
                )
                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    pCur?.let {
                        while (pCur.moveToNext()) {
                            val phoneNo: String = pCur.getString(
                                pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                                )
                            ).replace("\\D+".toRegex(), "")

                            Logging.debug(
                                PhoneContactFragment.TAG,
                                "Contact info: {ID: $id, Name: $name, Phone Number: $phoneNo}"
                            )

                            val pair = Pair(phoneNo, name)
                            if (!result.contains(pair)) {
                                result.add(pair)
                            }
                        }
                        pCur.close()
                    }
                }
            }
            fetchRemotePhoneContactInfo(result)
        }
    }

    fun triggerButtonClick(index: Int, phoneContact: PhoneContact) {
        if (phoneContact.isPendingFriendRequest) {
            cancelFriendRequest(index, phoneContact)
        } else {
            addFriend(index, phoneContact)
        }
    }

    private fun addFriend(index: Int, phoneContact: PhoneContact) {
        cd.add(
            friendRequestRepository.requestConnect(phoneContact.uid)
                .startWith(Completable.fromAction {
                    postFriendResultValue(
                        index,
                        Result.Loading()
                    )
                })
                .compose(RxHelper.applyObservableSchedulers())
                .compose(RxHelper.parseResponseData())
                .subscribe({ isSuccess ->
                    if (isSuccess) {
                        phoneContact.isPendingFriendRequest = true
                        phoneContact.isFriend = false
                        postFriendResultValue(index, Result.Success(phoneContact))
                    }
                }, {
                    postFriendResultValue(
                        index,
                        Result.Fail(R.string.msg_phone_contacts_send_friend_error_fragment)
                    )
                })
        )
    }

    private fun cancelFriendRequest(index: Int, phoneContact: PhoneContact) {
        cd.add(
            friendRequestRepository.cancelRequest(phoneContact.uid)
                .startWith(Completable.fromAction {
                    postFriendResultValue(
                        index,
                        Result.Loading()
                    )
                })
                .compose(RxHelper.applyObservableSchedulers())
                .compose(RxHelper.parseResponseData())
                .subscribe({ isSuccess ->
                    if (isSuccess) {
                        phoneContact.isPendingFriendRequest = false
                        phoneContact.isFriend = false
                        postFriendResultValue(index, Result.Success(phoneContact))
                    }
                }, {
                    postFriendResultValue(
                        index,
                        Result.Fail(R.string.msg_phone_contacts_send_friend_error_fragment)
                    )
                })
        )
    }

    private fun postFriendResultValue(index: Int, result: Result<PhoneContact>) {
        val map = (_friendRequestResult.value ?: mutableMapOf()).toMutableMap()

        map[index] = result

        _friendRequestResult.postValue(map)
    }

    private fun fetchRemotePhoneContactInfo(numbers: List<Pair<String, String>>) {
        cd.add(
            Observable.fromIterable(numbers)
                .startWith(Completable.fromAction { _phoneContacts.postValue(Result.Loading()) })
                .concatMapDelayError { pair ->
                    userRepository.fetchPhoneContactInfo(pair.first)
                        .compose(RxHelper.parseResponseData())
                        .map { result ->
                            result.contactNumber = pair.first
                            result.contactName = pair.second
                            result
                        }
                        .onErrorReturnItem(PhoneContact.NOT_FOUND)
                }
                .filter { contact -> contact != PhoneContact.NOT_FOUND }
                .toList()
                .compose(RxHelper.applySingleSchedulers())
                .subscribe({ contacts ->
                    _phoneContacts.postValue(Result.Success(contacts))
                }, {
                    _phoneContacts.postValue(Result.Fail(-1))
                })
        )
    }

    override fun onCleared() {
        super.onCleared()

        cd.dispose()
    }
}