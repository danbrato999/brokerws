package com.github.danbrato999.brokerws.models

import io.vertx.core.json.JsonObject

interface BrokerWsConnection<T> {
  fun source() : ConnectionSource
  fun messageHandler(handler: (T) -> Unit) : BrokerWsConnection<T>
  fun closeHandler(handler: (Void?) -> Unit) : BrokerWsConnection<T>
  fun sendJsonMessage(message: JsonObject) : BrokerWsConnection<T>
  fun close() : BrokerWsConnection<T>
}
