package com.example

import com.apollo.repro.BooksWithFieldQuery
import com.apollo.repro.StandaloneFieldQuery
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
        val (cache_only1, job1) = watch(query = BooksWithFieldQuery())
        val (fieldCacheOnly, job2) = watch(query = StandaloneFieldQuery())
        cache_only1.readValue().data shouldBe null // empty cache = empty response
//        fieldCacheOnly.readValue().data shouldBe null // empty cache = empty response todo

        val refreshed = refresh(query = BooksWithFieldQuery())
        refreshed.data shouldBe cache_only1.readValue().data
        fieldCacheOnly.readValue().data?.viewer?.justAField shouldBe refreshed.data?.viewer?.justAField

        cache_only1.cancel()
        fieldCacheOnly.cancel()
        job1.cancel()
        job2.cancel()
    }
}

private suspend fun <T> Channel<T>.readValue() = receiveAsFlow().first()
