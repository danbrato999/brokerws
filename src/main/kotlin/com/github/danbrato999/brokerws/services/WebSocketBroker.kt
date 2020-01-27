package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.core.json.JsonObject

interface WebSocketBroker {
  fun notifyNewConnection(source: ConnectionSource) : WebSocketBroker

  fun receiveMessage(message: JsonObject) : WebSocketBroker

  fun notifyConnectionClosed(source: ConnectionSource) : WebSocketBroker
}
