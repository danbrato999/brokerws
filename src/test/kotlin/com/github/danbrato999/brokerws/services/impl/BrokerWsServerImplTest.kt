package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.*
import io.vertx.core.*
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import org.mockito.Mockito.*

@ExtendWith(VertxExtension::class)
internal class BrokerWsServerImplTest {

  @Test
  fun testWebSocketHandling(vertx: Vertx, testContext: VertxTestContext) {
    val source = ConnectionSource("junit", "test001")
    val messageStoredCheckpoint = testContext.checkpoint()
    val newConnectionCheckpoint = testContext.checkpoint()

    val store = mock(WebSocketServerStore::class.java)
    `when`(store.store(anyNonNull())).then { messageStoredCheckpoint.flag() }

    val broker = mock(WebSocketBroker::class.java)
    `when`(broker.notifyNewConnection(source)).then {
      newConnectionCheckpoint.flag()
      broker
    }

    startServer(vertx, store, broker, testContext.succeeding {
      client(vertx, source, testContext.succeeding())
    })

    assertComplete(testContext)
  }

  @Test
  fun testMessageForwarding(vertx: Vertx, testContext: VertxTestContext) {
    val messagesCount = 5
    val source = ConnectionSource("junit", "test002")

    val expectedMessage = JsonObject()
      .put("source", source.toJson())
      .put("data", messageContent.encode())

    val messagesCheckpoint = testContext.checkpoint(messagesCount)

    val store = WsConnectionStoreImpl()
    val broker = mock(WebSocketBroker::class.java)

    `when`(broker.notifyConnectionClosed(anyNonNull())).thenReturn(broker)
    `when`(broker.receiveMessage(expectedMessage)).then {
      messagesCheckpoint.flag()
      broker
    }

    startServer(vertx, store, broker, testContext.succeeding {
      client(vertx, source, testContext.succeeding { ws ->
        repeat(messagesCount) {
          ws.writeTextMessage(messageContent.encode())
        }
      })
    })

    assertComplete(testContext)
  }

  @Test
  fun testOutgoingMessages(vertx: Vertx, testContext: VertxTestContext) {
    val targets = listOf(
      ConnectionSource("junit", "test005"),
      ConnectionSource("junit", "test007"),
      ConnectionSource("junit", "test008")
    )
    val messagesCheckpoint = testContext.checkpoint(targets.size)

    val store = WsConnectionStoreImpl()
    val broker = mock(WebSocketBroker::class.java)
    `when`(broker.receiveMessage(anyNonNull())).thenReturn(broker)
    `when`(broker.notifyConnectionClosed(anyNonNull())).thenReturn(broker)
    `when`(broker.notifyNewConnection(anyNonNull())).thenReturn(broker)

    startServer(vertx, store, broker, testContext.succeeding {
      val clientsFuture = (3..10).map { id ->
        ConnectionSource( "junit", "test00$id")
      }.map { source ->
        Future.future<WebSocket> { client(vertx, source, it) }
          .map { ws ->
              ws.textMessageHandler { msg ->
                if (targets.contains(source))
                  testContext.verify {
                    assertEquals(messageContent.encode(), msg, "Incorrect message content")
                    messagesCheckpoint.flag()
                  }
                else
                  testContext.failNow(AssertionError("Sent a message to the wrong socket"))
              }
          }
      }

      testContext.assertComplete(CompositeFuture.all(clientsFuture))
        .setHandler { ar ->
          if (ar.succeeded())
            store.broadcast(targets, messageContent)
          else
            testContext.failNow(ar.cause())
        }
    })

    assertComplete(testContext, 10)
  }
}
