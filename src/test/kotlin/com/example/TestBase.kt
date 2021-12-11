package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.*
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.okHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension

val client = OkHttpClient.Builder().build()

abstract class TestBase {
  @RegisterExtension
  @JvmField
  protected val mockWebServer = MockWebServerTestRule()

  protected lateinit var apollo: ApolloClient

  @BeforeEach
  internal fun setUp() {
    val lruNormalizedCacheFactory = MemoryCacheFactory()
    val cache = createInMemorySqlNormalizedCacheFactory()
    apollo = ApolloClient.Builder()
      .normalizedCache(
        normalizedCacheFactory = lruNormalizedCacheFactory.chain(cache),
        cacheKeyGenerator = IdBasedCacheKeyResolver(),
        writeToCacheAsynchronously = false
      )
      .serverUrl(mockWebServer.mockWebServer.url("/").toString())
      .okHttpClient(client)
      .build()
  }

  protected suspend fun <T> Channel<T>.assertNoEmission() {
    assert(withTimeoutOrNull(300) { receive() } == null)
  }

  protected fun <D : Query.Data> CoroutineScope.watch(
    query: Query<D>,
    fetchPolicy: FetchPolicy,
    refetchPolicy: FetchPolicy = fetchPolicy,
  ): Pair<Channel<ApolloResponse<D>>, Job> {
    val responses = Channel<ApolloResponse<D>>(capacity = Channel.UNLIMITED)

    val job = launch {
      apollo.query(query)
        .fetchPolicy(fetchPolicy)
        .refetchPolicy(refetchPolicy)
        .storePartialResponses(true)
        .toFlow()
        .catch { responses.close(it) }
        .collect { responses.send(it) }
    }
    job.invokeOnCompletion { responses.close() }

    return responses to job
  }

  protected suspend fun cacheString() = NormalizedCache.prettifyDump(apollo.apolloStore.accessCache(NormalizedCache::dump))
}

private fun createInMemorySqlNormalizedCacheFactory() = SqlNormalizedCacheFactory("jdbc:sqlite:")
