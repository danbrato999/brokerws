package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.core.json.JsonObject

interface WebSocketBaseStore {
  /**
   * Sends a message to all the expected connections
   *
   * @param targets List of possible connection sources
   * @param message Message to send to the connections
   */
  fun broadcast(targets: List<ConnectionSource>, message: JsonObject)

  /**
   * Deletes a connection from the store, if it exists
   *
   * @return true if the source was deleted
   */
  fun delete(source: ConnectionSource) : Boolean
}
