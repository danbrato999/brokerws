package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.WebSocketConnection
import com.github.danbrato999.brokerws.services.WebSocketBaseStore
import com.github.danbrato999.brokerws.services.WebSocketServerStore
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory

class WebSocketServerStoreImpl : WebSocketServerStore {
  private val connections = mutableSetOf<WebSocketConnection>()

  override fun store(connection: WebSocketConnection) : Boolean {
    val overridden = this.delete(connection.source)
    connections.add(connection)
    Logger.debug("Accepted new web socket connection -> ${connection.source}")
    return !overridden
  }

  override fun broadcast(targets: List<ConnectionSource>, message: JsonObject) : WebSocketBaseStore {
    Logger.debug("Broadcasting message to connections $targets, message $message")
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

  companion object {
    private val Logger = LoggerFactory.getLogger(WebSocketServerStore::class.java)
  }
}
