package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.*
import com.apollographql.apollo3.cache.normalized.api.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.okHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
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
                cacheKeyGenerator = IdBasedCacheKeyResolver,
                cacheResolver = IdBasedCacheKeyResolver,
                writeToCacheAsynchronously = false,
            )
//            .emitCacheMisses(true)
            .serverUrl(mockWebServer.mockWebServer.url("/").toString())
            .okHttpClient(client)
            .build()
    }

    protected suspend fun <T> Channel<T>.assertNoEmission() {
        assert(withTimeoutOrNull(300) { receive() } == null)
    }

    protected fun <D : Query.Data> CoroutineScope.watch(
        query: Query<D>,
    ): Pair<Channel<ApolloResponse<D>>, Job> {
        val responses = Channel<ApolloResponse<D>>(capacity = Channel.UNLIMITED)

        val job = launch {
            apollo.query(query)
                .fetchPolicy(FetchPolicy.CacheOnly)
                .refetchPolicy(FetchPolicy.CacheFirst)
                .storePartialResponses(true)
                .emitCacheMisses(true)
                .watch(fetchThrows = false, refetchThrows = true)
                .catch { responses.close(it) }
                .collect { responses.send(it) }
        }
        job.invokeOnCompletion { responses.close() }

        return responses to job
    }

    protected suspend fun <D : Query.Data> refresh(query: Query<D>) =
        apollo.query(query)
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .storePartialResponses(true)
            .execute()

    protected suspend fun cacheString() = NormalizedCache.prettifyDump(apollo.apolloStore.accessCache(NormalizedCache::dump))
}

private fun createInMemorySqlNormalizedCacheFactory() = SqlNormalizedCacheFactory("jdbc:sqlite:")
