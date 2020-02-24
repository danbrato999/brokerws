package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.BrokerWsConnection
import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.impl.BrokerWsDefaultStore
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

interface BrokerWsStore {
  /**
   * Adds a new connection to the store
   */
  fun store(connection: BrokerWsConnection<*>, handler: Handler<AsyncResult<String>>)

  /**
   * Tries to send a message to all the expected connections
   *
   * @param targets List of possible connection sources
   * @param message Message to send to the connections
   */
  fun broadcast(targets: List<ConnectionSource>, message: JsonObject)

  /**
   * Deletes a connection from the store, if it exists
   */
  fun deleteOne(source: ConnectionSource, handler: Handler<AsyncResult<String>>)

  /**
   * Closes a list of WebSocket connections from the server
   *
   * @param requestIds List of requestIds that identify the connections
   * @param message Message to send to the WebSocket before closing the connection
   */
  fun close(requestIds: List<String>, message: JsonObject?)

  /**
   * This function should allow to set a timer to check the status of the currently alive collections
   */
  fun healthCheck(vertx: Vertx, delay: Long)

  companion object {
    fun local(registry: ConnectionRegistry) : BrokerWsStore = BrokerWsDefaultStore(registry)
  }
}
