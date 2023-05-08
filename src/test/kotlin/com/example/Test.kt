package com.example

import com.apollo.repro.BooksByIDQuery
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class Test : TestBase() {

    @Test
    fun `cache only`() = runBlocking {
        mockWebServer.enqueue(response)
        val (cache_only1, job1) = watch(query = BooksByIDQuery(bookId = "fixture-id"))
        cache_only1.readValue().data shouldBe null // empty cache = empty response

        val refreshed = refresh(query = BooksByIDQuery(bookId = "fixture-id"))
        refreshed.data shouldBe cache_only1.readValue().data

        cache_only1.cancel()
        job1.cancel()
    }
}

private suspend fun <T> Channel<T>.readValue() = receiveAsFlow().first()
