package com.example

import com.apollographql.apollo3.cache.normalized.FetchPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class Test : TestBase() {

  @OptIn(ExperimentalTime::class)
  @Test
  fun `cache only`() = runBlocking {
    mockWebServer.enqueue(response)
    val (prefetch, prefetchJob) = watch(BooksQuery(), fetchPolicy = FetchPolicy.NetworkOnly, refetchPolicy = FetchPolicy.NetworkOnly)
    val (cache_only, firstJob) = watch(BooksQuery(), fetchPolicy = FetchPolicy.CacheOnly, refetchPolicy = FetchPolicy.CacheFirst)

    val receivedFirst = prefetch.receiveAsFlow().first()
    val receivedSecond = cache_only.receiveAsFlow().first()
    check(receivedFirst.data == receivedSecond.data)

    prefetch.cancel()
    cache_only.cancel()
    prefetchJob.cancel()
    firstJob.cancel()
  }
}
