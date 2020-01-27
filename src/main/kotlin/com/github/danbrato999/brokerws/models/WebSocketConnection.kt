package com.github.danbrato999.brokerws.models

import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject

data class WebSocketConnection(
  val source: ConnectionSource,
  private val ws: ServerWebSocket
) {
  fun sendMessage(message: JsonObject) : WebSocketConnection {
    ws.writeTextMessage(message.encode())
    return this
  }
}
