package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.WebSocketConnection

interface WebSocketServerStore : WebSocketBaseStore {
  /**
   * Adds a new connection to the store
   *
   * @param connection New WebSocket connection
   * @return True if the connection is new, false if overridden
   */
  fun store(connection: WebSocketConnection) : Boolean
}
