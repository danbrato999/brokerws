package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.json.JsonObject

@ProxyGen
interface WebSocketBroker {
  @Fluent
  fun notifyNewConnection(source: ConnectionSource) : WebSocketBroker

  @Fluent
  fun receiveMessage(message: JsonObject) : WebSocketBroker

  @Fluent
  fun notifyConnectionClosed(source: ConnectionSource) : WebSocketBroker
}
