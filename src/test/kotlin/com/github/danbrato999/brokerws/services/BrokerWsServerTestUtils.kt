package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.impl.BrokerWsHandlerImpl
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions
import org.mockito.Mockito
import java.util.concurrent.TimeUnit

private const val port = 16969
val messageContent: JsonObject = JsonObject().put("ping", "pong")

inline fun <reified T> anyNonNull(): T = Mockito.any<T>(T::class.java)

private val uriRegex = Regex("^/ws/(.*)/(.*)$")
private fun BrokerWsHandler.requestHandler() : Handler<HttpServerRequest> = Handler { request ->
  if (uriRegex matches request.uri()) {
    try {
      val matches = uriRegex.find(request.uri())!!
        .groupValues
        .drop(1)
      val source = ConnectionSource(matches.first(), matches.last())
      this.handle(source, request)
    } catch (e: Exception) {
      request
        .response()
        .setStatusCode(500)
        .end()
    }
  }
}

fun startServer(
  vertx: Vertx,
  store: WebSocketServerStore,
  broker: WebSocketBroker,
  handler: Handler<AsyncResult<HttpServer>>
) {
    val server = BrokerWsHandlerImpl(store, broker)
  vertx.createHttpServer()
    .requestHandler(server.requestHandler())
    .listen(port, handler)
}

fun startServer(
  vertx: Vertx,
  store: WebSocketServerStore,
  brokerAddress: String,
  handler: Handler<AsyncResult<HttpServer>>
) {
  Future.future<BrokerWsHandler> {
    BrokerWsHandler.create(vertx, store, brokerAddress, it)
  }
    .compose { wsHandler ->
      Future.future<HttpServer> {
        vertx.createHttpServer()
          .requestHandler(wsHandler.requestHandler())
          .listen(port, it)
      }
    }.setHandler(handler)
}

fun client(vertx: Vertx, source: ConnectionSource, handler: Handler<AsyncResult<WebSocket>>) {
  vertx.createHttpClient()
    .webSocket(port, "localhost", "/ws/${source.entity}/${source.id}", handler)
}

fun assertComplete(testContext: VertxTestContext, seconds: Long = 5) {
  Assertions.assertTrue(testContext.awaitCompletion(seconds, TimeUnit.SECONDS), "Test failed to complete")
}
