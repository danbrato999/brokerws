package com.github.danbrato999.brokerws.services.client

import com.github.danbrato999.brokerws.models.IncomingMessage
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen

@ProxyGen
interface BrokerWsMessageHandler {
  @Fluent
  fun handle(message: IncomingMessage) : BrokerWsMessageHandler
}
