package com.mqv.vmess.ui.validator

import android.util.Patterns

object InputValidator {
    @JvmStatic
    fun isLinkMessage(plaintText: String): Boolean {
        return Patterns.WEB_URL.matcher(plaintText).find()
    }

    @JvmStatic
    fun getLinkFromText(plaintText: String): String? {
        val links: MutableList<String> = ArrayList()
        val m = Patterns.WEB_URL.matcher(plaintText)

        while (m.find()) {
            val url: String = m.group()
            links.add(url)
        }

        return if (links.isEmpty()) null else links.first()
    }
}