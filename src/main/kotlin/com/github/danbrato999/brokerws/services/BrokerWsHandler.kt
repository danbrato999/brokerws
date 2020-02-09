package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.impl.BrokerWsTextHandler
import io.vertx.core.http.HttpServerRequest

interface BrokerWsHandler {

  fun handle(source: ConnectionSource, request: HttpServerRequest)

  companion object {
    fun create(
      store: BrokerWsStore,
      broker: WebSocketBroker
    ) : BrokerWsHandler = BrokerWsTextHandler(store, broker)
  }
}
