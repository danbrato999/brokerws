package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.RabbitMQBrokerConfig
import com.github.danbrato999.brokerws.services.impl.BrokerWsServerImpl
import com.github.danbrato999.brokerws.services.impl.RabbitMQBroker
import com.github.danbrato999.brokerws.services.impl.WsConnectionStoreImpl
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router

interface BrokerWsServer {

  fun router() : Router

  companion object {
    fun create(
      vertx: Vertx,
      rabbitMQBrokerConfig: RabbitMQBrokerConfig,
      handler: Handler<AsyncResult<BrokerWsServer>>
    ) {
      val wsStore = WsConnectionStoreImpl()

      Future.future<WebSocketBroker> {
        RabbitMQBroker.create(vertx, wsStore, rabbitMQBrokerConfig, it)
      }
        .map { broker ->
          BrokerWsServerImpl(vertx, wsStore, broker) as BrokerWsServer
        }
        .setHandler(handler)
    }
  }
}
