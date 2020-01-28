package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.WebSocketConnection
import com.github.danbrato999.brokerws.services.WebSocketBaseStore
import com.github.danbrato999.brokerws.services.WebSocketServerStore
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

class WsConnectionStoreImpl : WebSocketServerStore {
  private val connections = mutableSetOf<WebSocketConnection>()

  override fun store(connection: WebSocketConnection) : Boolean {
    val overridden = this.delete(connection.source)
    connections.add(connection)
    return !overridden
  }

  override fun broadcast(targets: List<ConnectionSource>, message: JsonObject) : WebSocketBaseStore {
    targets
      .mapNotNull { source ->
        connections.find { it.source == source }
      }.forEach { it.sendMessage(message) }

    return this
  }

  override fun delete(source: ConnectionSource, handler: Handler<AsyncResult<Boolean>>) : WebSocketBaseStore {
    handler.handle(
      Future.succeededFuture(
        this.delete(source)
      )
    )
    return this
  }

  private fun delete(source: ConnectionSource) : Boolean = connections.removeIf { it.source == source }
}
