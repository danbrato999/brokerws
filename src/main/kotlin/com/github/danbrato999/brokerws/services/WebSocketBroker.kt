package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.RabbitMQClientConfig
import com.github.danbrato999.brokerws.services.impl.RabbitMQBroker
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

interface WebSocketBroker {
  fun notifyNewConnection(source: ConnectionSource) : WebSocketBroker

  fun receiveMessage(message: JsonObject) : WebSocketBroker

  fun notifyConnectionClosed(source: ConnectionSource) : WebSocketBroker

  companion object {
    fun rabbitMqBroker(
      vertx: Vertx,
      store: BrokerWsStore,
      config: RabbitMQClientConfig,
      handler: Handler<AsyncResult<WebSocketBroker>>
    ) = RabbitMQBroker(store, config)
      .start(vertx, handler)
  }
}
