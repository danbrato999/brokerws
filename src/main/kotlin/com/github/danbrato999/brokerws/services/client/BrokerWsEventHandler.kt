package com.github.danbrato999.brokerws.services.client

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen

@ProxyGen
interface BrokerWsEventHandler {
  @Fluent
  fun handleNewConnection(source: ConnectionSource) : BrokerWsEventHandler

  @Fluent
  fun handleDisconnection(source: ConnectionSource) : BrokerWsEventHandler
}
