package com.github.danbrato999.brokerws.services.client

import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.core.json.JsonObject

interface BrokerWsWorker {
  fun sendMessage(source: ConnectionSource, message: JsonObject)
  fun closeConnections(connections: List<ConnectionSource>, message: JsonObject)
}
