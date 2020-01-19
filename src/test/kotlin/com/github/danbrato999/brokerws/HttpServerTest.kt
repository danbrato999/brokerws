package com.github.danbrato999.brokerws

import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Checkpoint
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.deploymentOptionsOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@DisplayName("Http server tests")
@ExtendWith(VertxExtension::class)
class HttpServerTest {
  private val defaultPort = 16969

  private val options = deploymentOptionsOf(
    config = JsonObject().put("port", defaultPort.toString())
  )
  private val messageContent = JsonObject().put("ping", "pong")

  @Test
  @DisplayName("Deployment with valid configurations")
  fun testServerDeployment(vertx: Vertx, testContext: VertxTestContext) {
    vertx.deployVerticle(HttpServer(), options, testContext.succeeding {
      testContext.completeNow()
    })
  }

  @Test
  @DisplayName("Deployment with missing configurations")
  fun testMissingConfigurationDeployment(vertx: Vertx, testContext: VertxTestContext) {
    vertx.deployVerticle(HttpServer(), testContext.failing {
      testContext.completeNow()
    })
  }

  @Test
  fun testWsConnection(vertx: Vertx, testContext: VertxTestContext) {
    val client = vertx.createHttpClient()
    val serverCheckpoint = testContext.checkpoint()
    val wsConnectionCheckpoint = testContext.checkpoint()

    vertx.deployVerticle(HttpServer(), options, testContext.succeeding<String> {
      serverCheckpoint.flag()
      client.webSocket(defaultPort, "localhost", "/ws/junit/test001", testContext.succeeding { ws ->
        wsConnectionCheckpoint.flag()
        testContext.verify {
          assertFalse(ws.isClosed, "WebSocket connection must be open")
        }
      })
    })
  }

  @Test
  fun testIncomingMessageForwarding(vertx: Vertx, testContext: VertxTestContext) {
    val messagesCount = 5
    val client = vertx.createHttpClient()
    val messagesCheckpoint = testContext.checkpoint(messagesCount)

    vertx.eventBus().consumer<JsonObject>(INCOMING_MESSAGES_ADDRESS) {
      testContext.verify {
        assertEquals("junit.test002", it.body().getString("source"), "Wrong source for WebSocket message")
        assertEquals(messageContent.encode(), it.body().getString("data"))
        messagesCheckpoint.flag()
      }
    }

    vertx.deployVerticle(HttpServer::class.java.name, options, testContext.succeeding {
      client.webSocket(defaultPort, "localhost", "/ws/junit/test002", testContext.succeeding { ws ->
        repeat(messagesCount) {
          ws.writeTextMessage(messageContent.encode())
        }
      })
    })

    assertTrue(testContext.awaitCompletion(10, TimeUnit.SECONDS), "Test failed to complete")
  }

  @Test
  fun testOutgoingMessages(vertx: Vertx, testContext: VertxTestContext) {
    val messagesCount = 5
    val client = vertx.createHttpClient()
    val messagesCheckpoint = testContext.checkpoint(messagesCount)

    vertx.deployVerticle(HttpServer::class.java.name, options, testContext.succeeding {
      client.webSocket(defaultPort, "localhost", "/ws/junit/test003", testContext.succeeding { ws ->
        ws.textMessageHandler(successMessageHandler(testContext, messagesCheckpoint))
        repeat(5) { sendOutgoingMessage(vertx, listOf("junit.test003")) }
      })
    })

    assertTrue(testContext.awaitCompletion(10, TimeUnit.SECONDS), "Test failed to complete")
  }

  @Test
  fun testMultiTargetOutgoingMessage(vertx: Vertx, testContext: VertxTestContext) {
    val targets = listOf("junit.test005", "junit.test0010", "junit.test0014")
    val messagesCheckpoint = testContext.checkpoint(targets.size)
    val successHandler = successMessageHandler(testContext, messagesCheckpoint)
    val failureHandler = Handler<String> {
      testContext.failNow(AssertionError("Sent a message to the wrong socket"))
    }

    vertx.deployVerticle(HttpServer::class.java.name, options, testContext.succeeding {
      val clientsFuture = (4..15).map { clientId ->
        Future.future<WebSocket> { promise ->
          vertx.createHttpClient()
            .webSocket(defaultPort, "localhost", "/ws/junit/test00${clientId}", promise)
        }.map { ws ->
          if (targets.contains("junit.test00$clientId"))
            ws.textMessageHandler(successHandler)
          else
            ws.textMessageHandler(failureHandler)
        }
      }

      testContext.assertComplete(CompositeFuture.all(clientsFuture))
        .setHandler { ar ->
          if (ar.failed())
            testContext.failNow(ar.cause())
          else
            sendOutgoingMessage(vertx, targets)
        }
    })

    assertTrue(testContext.awaitCompletion(10, TimeUnit.SECONDS), "Test failed to complete")
  }

  private fun sendOutgoingMessage(vertx: Vertx, targets: List<String>) {
    val message = OutgoingMessage(targets, messageContent)
    vertx.eventBus().send(OUTGOING_MESSAGES_ADDRESS, JsonObject.mapFrom(message))
  }

  private fun successMessageHandler(context: VertxTestContext, checkpoint: Checkpoint) : Handler<String> = Handler { msg ->
    context.verify {
      assertEquals(messageContent.encode(), msg, "Incorrect message content")
      checkpoint.flag()
    }
  }
}
