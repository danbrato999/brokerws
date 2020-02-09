package com.github.danbrato999.brokerws.models

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@DataObject
data class IncomingMessage(val source: ConnectionSource, val data: String) {
  constructor(json: JsonObject) : this(ConnectionSource(json.getJsonObject("source")), json.getString("data"))
  fun toJson() : JsonObject = JsonObject.mapFrom(this)
}

data class OutgoingMessage(val targets: List<ConnectionSource>, val data: JsonObject) {
  constructor(json: JsonObject) : this(
    json.getJsonArray("targets").map { ConnectionSource(it as JsonObject) },
    json.getJsonObject("data")
  )
  fun toJson() : JsonObject = JsonObject.mapFrom(this)
}

data class RabbitMQExchangeQueueConfig(
  val exchange: String,
  val exchangeType: String = "fanout",
  val queue: String = "",
  val durable: Boolean = false
) {
  constructor(json: JsonObject) : this(
    json.getString("exchange"),
    json.getString("exchangeType", "fanout"),
    json.getString("queue", ""),
    json.getBoolean("durable", false)
  )

  fun toJson(): JsonObject = JsonObject.mapFrom(this)
}

enum class WsConnectionEventType {
  Connection,
  Disconnection
}

@DataObject
data class BrokerWsConnectionEvent(val type: WsConnectionEventType, val source: ConnectionSource) {
  constructor(json: JsonObject) : this(
    WsConnectionEventType.valueOf(json.getString("type")),
    ConnectionSource(json.getJsonObject("source"))
  )
  fun toJson() : JsonObject = JsonObject.mapFrom(this)
}
