package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.TextWebSocket
import com.github.danbrato999.brokerws.services.BrokerWsHandler
import com.github.danbrato999.brokerws.services.BrokerWsStore
import com.github.danbrato999.brokerws.services.WebSocketBroker
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class BrokerWsTextHandler(
  private val store: BrokerWsStore,
  private val webSocketBroker: WebSocketBroker
) : BrokerWsHandler {
  override fun handle(source: ConnectionSource, request: HttpServerRequest) {
    val webSocket = request
      .upgrade()

    val conn = TextWebSocket(source, webSocket)
      .messageHandler { msg ->
        val detailedMessage = json {
          obj(
            "source" to JsonObject.mapFrom(source),
            "data" to msg
          )
        }

        webSocketBroker.receiveMessage(detailedMessage)
      }
      .closeHandler {
        store.deleteOne(source, Handler {
          webSocketBroker.notifyConnectionClosed(source)
        })
      }

    store.store(conn, Handler { ar ->
      if (ar.succeeded()) {
        webSocketBroker.notifyNewConnection(source)
        webSocket.accept()
      } else
        terminate(webSocket, request)
    })
  }

  private fun terminate(ws: ServerWebSocket, request: HttpServerRequest) {
    ws.close()
    request
      .response()
      .setStatusCode(400)
      .end()
  }
}
