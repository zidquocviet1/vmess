package com.mqv.vmess.util.views

import android.view.ViewStub

class Stub<T>(private val viewStub: ViewStub) {
    private var view: T? = null

    fun get(): T {
        if (view == null) {
            view = viewStub.inflate() as T
        }
        return view!!
    }

    fun resolved() = view != null
}