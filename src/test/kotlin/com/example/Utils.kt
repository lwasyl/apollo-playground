package com.example

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class MockWebServerTestRule : BeforeEachCallback, AfterEachCallback {

    lateinit var mockWebServer: MockWebServer
        private set

    fun enqueue(@Language("JSON") response: String) =
        mockWebServer.enqueue(MockResponse().setBody(response).setBodyDelay(10, TimeUnit.MILLISECONDS))

    override fun beforeEach(context: ExtensionContext?) {
        println("Starting web server")
        mockWebServer = MockWebServer()
        mockWebServer.dispatcher = object : QueueDispatcher() {
            init {
                setFailFast(true)
            }

            override fun dispatch(request: RecordedRequest): MockResponse {
                println("Dispatching ${request.getHeader("X-APOLLO-OPERATION-NAME")}")
                return super.dispatch(request)
            }
        }
        mockWebServer.start()
    }

    override fun afterEach(context: ExtensionContext?) {
        println("Closing web server")
        mockWebServer.close()
    }
}

internal object IdBasedCacheKeyResolver : CacheKeyGenerator, CacheKeyResolver() {

    private const val FIELD_ID = "id"
    private const val FIELD_IDS = "ids"
    override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext) =
        obj[FIELD_ID]?.asCacheKey()

    override fun cacheKeyForField(field: CompiledField, variables: Executable.Variables): CacheKey? =
        field.resolveArgument(FIELD_ID, variables)?.asCacheKey()

    override fun listOfCacheKeysForField(field: CompiledField, variables: Executable.Variables): List<CacheKey?>? {
        val ids = field.resolveArgument(FIELD_IDS, variables)

        return if (ids is List<*>) {
            ids.map { it?.asCacheKey() }
        } else {
            null
        }
    }

    private fun Any.asCacheKey() =
        (this as? String)
            ?.takeUnless(String::isBlank)
            ?.let(::CacheKey)

}

fun immediateExecutorService(): ExecutorService {
    return object : AbstractExecutorService() {
        override fun shutdown() = Unit

        override fun shutdownNow(): List<Runnable>? = null

        override fun isShutdown(): Boolean = false

        override fun isTerminated(): Boolean = false

        @Throws(InterruptedException::class)
        override fun awaitTermination(l: Long, timeUnit: TimeUnit): Boolean = false

        override fun execute(runnable: Runnable) = runnable.run()
    }
}
