package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionBuilder
import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.BrokerWsServer
import com.github.danbrato999.brokerws.services.WebSocketBroker
import com.github.danbrato999.brokerws.services.WebSocketServerStore
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class BrokerWsServerImpl(
  private val vertx: Vertx,
  private val webSocketServerStore: WebSocketServerStore,
  private val webSocketBroker: WebSocketBroker
) : BrokerWsServer {

  override fun router(): Router {
    val router = Router.router(vertx)
    router
      .route(HttpMethod.GET, "/ws/:entity_type/:id")
      .handler(this::handleWebSocketConnection)

    return router
  }

  private fun handleWebSocketConnection(routingContext: RoutingContext) {
    val source = ConnectionSource(
      routingContext.request().getParam("entity_type"),
      routingContext.request().getParam("id")
    )

    val connection = ConnectionBuilder()
      .withConnection(routingContext.request().upgrade())
      .withSource(source)
      .withMessageHandler {
        webSocketBroker.receiveMessage(it)
      }.withCloseHandler {
        if (webSocketServerStore.delete(it))
          webSocketBroker.notifyConnectionClosed(it)
      }.build()


    webSocketBroker.notifyNewConnection(source)
    webSocketServerStore.store(connection)
  }
}
