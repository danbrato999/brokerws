package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.BrokerWsConnection
import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.BrokerWsStore
import com.github.danbrato999.brokerws.services.ConnectionRegistry
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class BrokerWsDefaultStore(
  private val registry: ConnectionRegistry,
  private val connections: MutableList<BrokerWsConnection<*>> = mutableListOf()
) : BrokerWsStore {
  override fun store(connection: BrokerWsConnection<*>, handler: Handler<AsyncResult<String>>) {
    if (connections.findByRequestId(connection.source()) != null)
      handler.handle(Future.failedFuture("Connection ${connection.source()} already exists"))
    else {
      connections.add(connection)
      registry.add(connection.source(), handler)
    }
  }

  override fun broadcast(targets: List<ConnectionSource>, message: JsonObject) {
    targets
      .flatMap { target ->
        if (target.requestId == "")
          connections.filter { it.source().entityId == target.entityId }
        else
          listOf(connections.findByRequestId(target))
      }
      .forEach { conn -> conn?.sendJsonMessage(message) }
  }

  override fun deleteOne(source: ConnectionSource, handler: Handler<AsyncResult<String>>) {
    connections.removeIf { it.source() == source }
    registry.delete(source, handler)
  }

  override fun close(requestIds: List<String>, message: JsonObject?) {
    connections
      .filter { it.source().requestId in requestIds }
      .forEach { conn ->
        if (message != null)
          conn.sendJsonMessage(message)

        conn.close()
      }
  }

  override fun healthCheck(vertx: Vertx, delay: Long) {
    vertx.setTimer(delay) {
      registry.keepAlive(
        connections
          .map { it.source() },
        Handler {
          if (it.failed())
            Logger.error("Failed to refresh connection status", it.cause())

          healthCheck(vertx, delay)
        }
      )
    }
  }

  companion object {
    private val Logger = LoggerFactory.getLogger(BrokerWsStore::class.java)
    private fun MutableList<BrokerWsConnection<*>>.findByRequestId(
      source: ConnectionSource
    ): BrokerWsConnection<*>? = this.find { it.source().requestId == source.requestId }
  }
}
