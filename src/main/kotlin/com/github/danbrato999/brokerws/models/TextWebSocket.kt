package com.github.danbrato999.brokerws.models

import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject

class TextWebSocket(
  private val source: ConnectionSource,
  private val ws: ServerWebSocket
) : BrokerWsConnection<String> {

  override fun source(): ConnectionSource = source

  override fun messageHandler(handler: (String) -> Unit): BrokerWsConnection<String> {
    ws.textMessageHandler(handler)
    return this
  }

  override fun closeHandler(handler: (Void?) -> Unit): BrokerWsConnection<String> {
    ws.closeHandler(handler)
    return this
  }

  override fun sendJsonMessage(message: JsonObject): BrokerWsConnection<String> {
    ws.writeTextMessage(message.encode())
    return this
  }

  override fun close(): BrokerWsConnection<String> {
    ws.close()
    return this
  }
}
