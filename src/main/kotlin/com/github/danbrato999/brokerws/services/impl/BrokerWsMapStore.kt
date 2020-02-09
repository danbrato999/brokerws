package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.BrokerWsConnection
import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.BrokerWsStore
import com.github.danbrato999.brokerws.services.ConnectionRegistry
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

class BrokerWsMapStore(
  private val registry: ConnectionRegistry,
  private val map: MutableMap<String, BrokerWsConnection<*>> = mutableMapOf()
) : BrokerWsStore {
  override fun store(connection: BrokerWsConnection<*>, handler: Handler<AsyncResult<String>>) {
    if (map.containsKey(connection.source().requestId))
      handler.handle(Future.failedFuture("Connection ${connection.source()} already exists"))
    else {
      map[connection.source().requestId] = connection
      registry.add(connection.source(), handler)
    }
  }

  override fun broadcast(targets: List<ConnectionSource>, message: JsonObject) {
    targets
      .forEach {
        map[it.entityId]
          ?.sendJsonMessage(message)
      }
  }

  override fun deleteOne(source: ConnectionSource, handler: Handler<AsyncResult<String>>) {
    map.remove(source.entityId)
    registry.delete(source, handler)
  }

  override fun close(requestIds: List<String>, message: JsonObject) {
    requestIds
      .forEach { id ->
        map[id]
          ?.sendJsonMessage(message)
          ?.close()
      }
  }
}
