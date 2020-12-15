package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.coroutines.toFlow
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension

abstract class TestBase {
  @RegisterExtension
  @JvmField
  protected val mockWebServer = MockWebServerTestRule()

  protected lateinit var apollo: ApolloClient

  @BeforeEach
  internal fun setUp() {
    apollo = ApolloClient.builder()
      .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdBasedCacheKeyResolver())
      .serverUrl(mockWebServer.mockWebServer.url("/"))
      .dispatcher(immediateExecutorService())
      .okHttpClient(OkHttpClient.Builder().dispatcher(Dispatcher(immediateExecutorService())).build())
      .build()
  }

  protected suspend fun <T> Channel<T>.assertNoEmission() {
    assert(withTimeoutOrNull(300) { receive() } == null)
  }

  protected fun <D : Operation.Data> CoroutineScope.watch(
    query: Query<D, D, *>,
    networkOnly: Boolean = false
  ): Pair<Channel<Response<D>>, Job> {
    val responses = Channel<Response<D>>(capacity = Channel.UNLIMITED)

    val job = launch {
      apollo.query(query)
        .toBuilder()
        .apply {
          if (networkOnly) {
            responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
          } else {
            responseFetcher(ApolloResponseFetchers.CACHE_FIRST)
          }
        }
        .build()
        .watcher()
        .toFlow()
        .collect {
          responses.sendBlocking(it)
        }
    }

    return responses to job
  }

  protected fun cacheString() = NormalizedCache.prettifyDump(apollo.apolloStore.normalizedCache().dump())
}
