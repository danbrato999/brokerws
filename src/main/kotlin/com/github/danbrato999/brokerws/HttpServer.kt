package com.github.danbrato999.brokerws

import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

class HttpServer : AbstractVerticle() {
  private val connectionsMap: MutableMap<String, ServerWebSocket> = mutableMapOf()

  override fun start(startPromise: Promise<Void>) {
    initOutgoingHandler()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.route(HttpMethod.GET, "/ws/:id").handler(this::connect)

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(config().getString("port").toInt()) { http ->
        if (http.succeeded())
          startPromise.complete()
        else
          startPromise.fail(http.cause());
      }
  }

  private fun connect(rc: RoutingContext) {
    val connection = rc.request().upgrade()
    val id = rc.request().getParam("id")
    val handler = Handler<String> { message ->
      val incomingMessage = IncomingMessage(id, JsonObject(message))
      vertx.eventBus().send(INCOMING_MESSAGES_ADDRESS, incomingMessage.toJson())
    }

    connection.textMessageHandler(handler)
    connectionsMap[id] = connection
  }

  private fun initOutgoingHandler() {
    vertx.eventBus().consumer<JsonObject>(OUTGOING_MESSAGES_ADDRESS) {
      val message = OutgoingMessage(it.body())
      message
        .targets
        .forEach { key ->
          connectionsMap[key]?.writeTextMessage(message.data.encode())
        }
    }
  }
}
