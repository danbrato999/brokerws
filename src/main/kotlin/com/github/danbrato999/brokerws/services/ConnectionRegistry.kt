package com.github.danbrato999.brokerws.services

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

@ProxyGen
interface ConnectionRegistry {
  @Fluent
  fun add(source: ConnectionSource, handler: Handler<AsyncResult<String>>) : ConnectionRegistry

  @Fluent
  fun list(handler: Handler<AsyncResult<List<ConnectionSource>>>) : ConnectionRegistry

  @Fluent
  fun findByRequestId(requestId: String, handler: Handler<AsyncResult<ConnectionSource?>>) : ConnectionRegistry

  @Fluent
  fun findByEntityId(entityId: String, handler: Handler<AsyncResult<List<ConnectionSource>>>) : ConnectionRegistry

  @Fluent
  fun delete(source: ConnectionSource, handler: Handler<AsyncResult<String>>) : ConnectionRegistry

  @Fluent
  fun keepAlive(connections: List<ConnectionSource>, handler: Handler<AsyncResult<Int>>) : ConnectionRegistry
}
