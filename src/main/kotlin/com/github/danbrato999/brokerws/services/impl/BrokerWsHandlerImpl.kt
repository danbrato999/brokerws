package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionBuilder
import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.BrokerWsHandler
import com.github.danbrato999.brokerws.services.WebSocketBroker
import com.github.danbrato999.brokerws.services.WebSocketServerStore
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest

class BrokerWsHandlerImpl(
  private val webSocketServerStore: WebSocketServerStore,
  private val webSocketBroker: WebSocketBroker
) : BrokerWsHandler {
  override fun handle(source: ConnectionSource, request: HttpServerRequest) {
    val connection = ConnectionBuilder()
      .withConnection(request.upgrade())
      .withSource(source)
      .withMessageHandler {
        webSocketBroker.receiveMessage(it)
      }.withCloseHandler {
        webSocketServerStore.delete(it, Handler { ar ->
          if (ar.succeeded() && ar.result())
            webSocketBroker.notifyConnectionClosed(it)
        })
      }.build()

    webSocketBroker.notifyNewConnection(source)
    webSocketServerStore.store(connection)
  }
}
