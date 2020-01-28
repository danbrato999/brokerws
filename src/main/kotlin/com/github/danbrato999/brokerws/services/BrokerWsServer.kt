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
import io.vertx.serviceproxy.ServiceProxyBuilder

interface BrokerWsServer {

  fun router() : Router

  companion object {
    fun create(
      vertx: Vertx,
      store: WebSocketServerStore,
      rabbitMQBrokerConfig: RabbitMQBrokerConfig,
      handler: Handler<AsyncResult<BrokerWsServer>>
    ) {
      Future.future<WebSocketBroker> {
        RabbitMQBroker.create(vertx, store, rabbitMQBrokerConfig, it)
      }
        .map { broker ->
          BrokerWsServerImpl(vertx, store, broker) as BrokerWsServer
        }
        .setHandler(handler)
    }

    fun create(
      vertx: Vertx,
      store: WebSocketServerStore,
      brokerAddress: String,
      handler: Handler<AsyncResult<BrokerWsServer>>
    ) {
      val broker = ServiceProxyBuilder(vertx)
        .setAddress(brokerAddress)
        .build(WebSocketBroker::class.java)

      Future.succeededFuture(BrokerWsServerImpl(vertx, store, broker) as BrokerWsServer)
        .setHandler(handler)
    }
  }
}
