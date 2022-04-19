package com.mqv.vmess

import android.util.Patterns
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.stream.Collectors

@RunWith(MockitoJUnitRunner::class)
class InputLinkMessageValidator {

    private val links = arrayListOf(
        "https://www.facebook.com",
        "https://app-1.number123.com",
        "http://facebook.com",
        "ftp://facebook.com",
        "http://localhost:3000",
        "localhost:3000/",
        "unitedkingdomurl.co.uk",
        "this.is.a.url.com/its/still=going?wow",
        "shop.facebook.org",
        "app.number123.com",
        "app1.number123.com",
        "app-1.numbEr123.com",
        "app.dashes-dash.com",
        "www.facebook.com",
        "facebook.com",
        "fb.com/hello_123",
        "fb.com/hel-lo",
        "fb.com/hello/goodbye",
        "fb.com/hello/goodbye?okay",
        "fb.com/hello/goodbye?okay=alright",
        "Hello www.google.com World http://yahoo.com",
        "https://www.google.com.tr/admin/subPage?qs1=sss1&qs2=sss2&qs3=sss3#Services",
        "https://google.com.tr/test/subPage?qs1=sss1&qs2=sss2&qs3=sss3#Services",
        "http://google.com/test/subPage?qs1=sss1&qs2=sss2&qs3=sss3#Services",
        "ftp://google.com/test/subPage?qs1=sss1&qs2=sss2&qs3=sss3#Services",
        "www.google.com.tr/test/subPage?qs1=sss1&qs2=sss2&qs3=sss3#Services",
        "www.google.com/test/subPage?qs1=sss1&qs2=sss2&qs3=sss3#Services",
        "drive.google.com/test/subPage?qs1=sss1&qs2=sss2&qs3=sss3#Services",
        "https://www.example.pl",
    )

    private val messages = arrayListOf(
        "ban da gui mot duong dan",
        "coi cai nay ne https://www.example.pl",
        "mai quoc viet",
        "51800954"
    )


    @Test
    fun howManyLinkIsSatisfied() {
        println("Total link to test: ${links.size}")

        val satisfiedLink = links.stream()
            .filter { l -> Patterns.WEB_URL.matcher(l).find() }
            .collect(Collectors.toList())

        println("The number of links are satisfied: ${satisfiedLink.size}")

        assertEquals(satisfiedLink.size, 21)
    }

    @Test
    fun getLinkFromTextMessage() {
//
//        val satisfiedLink = messages.stream()
//            .filter { l -> InputValidator.isLinkMessage(l) }
//            .collect(Collectors.toList())
//
//        satisfiedLink.forEach { s ->
//            val link = InputValidator.getLinkFromText(s)
//            println(link)
//        }
//
//        assertEquals(satisfiedLink.size, 1)
    }
}