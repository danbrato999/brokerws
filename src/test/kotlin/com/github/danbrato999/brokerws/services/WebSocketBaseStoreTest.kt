package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Checkpoint
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.serviceproxy.ServiceBinder
import io.vertx.serviceproxy.ServiceProxyBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class WebSocketBaseStoreTest {

  @Test
  fun testProxyStore(vertx: Vertx, testContext: VertxTestContext) {
    val checkpoint = testContext.checkpoint(3)
    val store = DummyStore(checkpoint)

    ServiceBinder(vertx)
      .setAddress(STORE_ADDRESS)
      .register(WebSocketBaseStore::class.java, store)

    val proxy = ServiceProxyBuilder(vertx)
      .setAddress(STORE_ADDRESS)
      .build(WebSocketBaseStore::class.java)

    proxy.broadcast(
      listOf(
        ConnectionSource("junit", "test001"),
        ConnectionSource("junit", "test002")
      ),
      messageContent
    )

    proxy.delete(ConnectionSource("junit", "test003"), testContext.succeeding())

    testContext.completeNow()
  }

  companion object {
    const val STORE_ADDRESS = "test.services.store"

    class DummyStore(private val checkpoint: Checkpoint) : WebSocketBaseStore {
      override fun broadcast(targets: List<ConnectionSource>, message: JsonObject): WebSocketBaseStore  = apply {
        if (message == messageContent)
          repeat(targets.size) { checkpoint.flag() }
      }

      override fun delete(source: ConnectionSource, handler: Handler<AsyncResult<Boolean>>): WebSocketBaseStore = apply {
        checkpoint.flag()
        handler.handle(Future.succeededFuture(true))
      }
    }
  }
}
