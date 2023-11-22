package com.example

import com.apollo.repro.SearchRecommendedBooksQuery
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.apolloStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class Test : TestBase() {

    @Test
    fun `cache only`() = runBlocking {
        val query = SearchRecommendedBooksQuery(bookId = "fixture-id")

        fun cacheDump() = NormalizedCache.prettifyDump(
            runBlocking { apollo.apolloStore.accessCache(NormalizedCache::dump) },
        )
        runBlocking {
            val (cache_only1, job1) = watch(query = query)
            cache_only1.readValue().data shouldBe null

            mockWebServer.enqueue(responseWithData)
            val refresh1 = refresh(query = query)
            val message = cacheDump()
            println(message)
            refresh1.data shouldBe cache_only1.readValue().data

            val (cache_only2, job2) = watch(query = query)
            cache_only2.readValue().data shouldBe SearchRecommendedBooksQuery.Data(
                viewer = SearchRecommendedBooksQuery.Viewer(
                    id = "viewerId",
                    similarBook = SearchRecommendedBooksQuery.SimilarBook(
                        id = "fixture-id",
                        name = "ok-2",
                    ),
                ),
            )

            mockWebServer.enqueue(responseWithNull)
            val refresh2 = refresh(query = query)
            refresh2.data shouldBe cache_only2.readValue().data

            cache_only1.cancel()
            job1.cancel()
            cache_only2.cancel()
            job2.cancel()
        }
    }
}

private suspend fun <T> Channel<T>.readValue() = receiveAsFlow().first()
