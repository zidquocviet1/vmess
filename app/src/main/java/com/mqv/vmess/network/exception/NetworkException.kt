package com.mqv.vmess.network.exception

import com.mqv.vmess.R

class NetworkException : Exception() {
    val stringRes get() = R.string.error_network_connection
}