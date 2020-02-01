package com.github.danbrato999.brokerws.services.client

import com.github.danbrato999.brokerws.models.BrokerWsConnectionEvent
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen

@ProxyGen
interface BrokerWsEventHandler {
  @Fluent
  fun handle(event: BrokerWsConnectionEvent) : BrokerWsEventHandler
}
