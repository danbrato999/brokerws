package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

@ProxyGen
interface WebSocketBaseStore {
  /**
   * Sends a message to all the expected connections
   *
   * @param targets List of possible connection sources
   * @param message Message to send to the connections
   */
  @Fluent
  fun broadcast(targets: List<ConnectionSource>, message: JsonObject) : WebSocketBaseStore

  /**
   * Deletes a connection from the store, if it exists
   *
   * @return true if the source was deleted
   */
  @Fluent
  fun delete(source: ConnectionSource, handler: Handler<AsyncResult<Boolean>>) : WebSocketBaseStore
}
