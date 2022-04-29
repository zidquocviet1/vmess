package com.mqv.vmess.ui.data

import com.mqv.vmess.util.DateTimeHelper.toLong
import java.time.LocalDateTime
import com.mqv.vmess.data.result.Result

data class SearchResultState<T>(
    val result: Result<T>,
    val searchTime: Long = LocalDateTime.now().toLong()
)
