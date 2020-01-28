package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.impl.WebSocketServerStoreImpl
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Checkpoint
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.serviceproxy.ServiceBinder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
internal class BrokerWsHandlerTest {

  @Test
  fun testProxyBroker(vertx: Vertx, testContext: VertxTestContext) {
    val messageCheckpoint = testContext.checkpoint()
    ServiceBinder(vertx)
      .setAddress(BROKER_ADDRESS)
      .register(WebSocketBroker::class.java, DummyBroker(messageCheckpoint))


    startServer(vertx, WebSocketServerStoreImpl(), BROKER_ADDRESS, testContext.succeeding {
      client(vertx, ConnectionSource("junit", "test001"), testContext.succeeding { ws ->
        ws.writeTextMessage(messageContent.encode())
      })
    })

    assertComplete(testContext)
  }

  companion object {
    private const val BROKER_ADDRESS = "services.test.broker"

    class DummyBroker(private val messageCheckpoint: Checkpoint) : WebSocketBroker {
      override fun notifyNewConnection(source: ConnectionSource): WebSocketBroker = this

      override fun receiveMessage(message: JsonObject): WebSocketBroker = apply {
        messageCheckpoint.flag()
      }

      override fun notifyConnectionClosed(source: ConnectionSource): WebSocketBroker = this
    }
  }
}
