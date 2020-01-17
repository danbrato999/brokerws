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
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.slf4j.LoggerFactory

class HttpServer : AbstractVerticle() {
  private val connectionsMap: MutableMap<String, ServerWebSocket> = mutableMapOf()

  override fun start(startPromise: Promise<Void>) {
    initOutgoingHandler()

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.route(HttpMethod.GET, "/ws/:entity_type/:id").handler(this::connect)

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
    val uid = uniqueId(
      rc.request().getParam("entity_type"),
      rc.request().getParam("id")
    )

    connection.textMessageHandler(textMessageHandler(uid))
    connection.close {
      Logger.debug("Connection $uid closed")
      connectionsMap.remove(uid)
    }

    connectionsMap[uid] = connection

    Logger.debug("Received new WebSocket connection for $uid")
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

  private fun textMessageHandler(uid: String) : Handler<String> = Handler { wsMessage ->
    val completeMessage = json {
      obj(
        "source" to uid,
        "data" to wsMessage
      )
    }
    vertx.eventBus().send(INCOMING_MESSAGES_ADDRESS, completeMessage)
  }

  companion object {
    private val Logger = LoggerFactory.getLogger(HttpServer::class.java)

    private fun uniqueId(entityType: String, id: String) = "$entityType.$id"
  }
}
