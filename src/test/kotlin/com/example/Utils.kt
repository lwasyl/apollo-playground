package com.example

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
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

  fun enqueue(@Language("JSON") response: String) = mockWebServer.enqueue(MockResponse().setBody(response))

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

internal class IdBasedCacheKeyResolver : CacheKeyResolver() {

  override fun fromFieldRecordSet(field: ResponseField, recordSet: Map<String, Any>): CacheKey =
    (recordSet["id"] as? String).asCacheKey()

  override fun fromFieldArguments(field: ResponseField, variables: Operation.Variables): CacheKey =
    (field.resolveArgument("id", variables) as? String).asCacheKey()

  private fun String?.asCacheKey(): CacheKey =
    takeIf { !it.isNullOrBlank() }
      ?.let { CacheKey.from(it) }
      ?: CacheKey.NO_KEY
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
