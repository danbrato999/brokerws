package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.RabbitMQBrokerConfig
import com.github.danbrato999.brokerws.services.impl.BrokerWsHandlerImpl
import com.github.danbrato999.brokerws.services.impl.RabbitMQBroker
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.serviceproxy.ServiceProxyBuilder

interface BrokerWsHandler {

  fun handle(source: ConnectionSource, request: HttpServerRequest)

  companion object {
    fun create(
      vertx: Vertx,
      store: WebSocketServerStore,
      rabbitMQBrokerConfig: RabbitMQBrokerConfig,
      handler: Handler<AsyncResult<BrokerWsHandler>>
    ) {
      Future.future<WebSocketBroker> {
        RabbitMQBroker(store, rabbitMQBrokerConfig)
          .start(vertx, it)
      }
        .map { broker ->
          BrokerWsHandlerImpl(store, broker) as BrokerWsHandler
        }
        .setHandler(handler)
    }

    fun create(
      vertx: Vertx,
      store: WebSocketServerStore,
      brokerAddress: String,
      handler: Handler<AsyncResult<BrokerWsHandler>>
    ) {
      val broker = ServiceProxyBuilder(vertx)
        .setAddress(brokerAddress)
        .build(WebSocketBroker::class.java)

      Future.succeededFuture(BrokerWsHandlerImpl(store, broker) as BrokerWsHandler)
        .setHandler(handler)
    }
  }
}
