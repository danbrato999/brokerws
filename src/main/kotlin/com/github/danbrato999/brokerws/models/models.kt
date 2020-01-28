package com.github.danbrato999.brokerws.models

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@DataObject
data class ConnectionSource(val entity: String, val id: String) {
  constructor(json: JsonObject) : this(json.getString("entity"), json.getString("id"))
  fun toJson() : JsonObject = JsonObject.mapFrom(this)
}

data class OutgoingMessage(val targets: List<ConnectionSource>, val data: JsonObject) {
  constructor(json: JsonObject) : this(
    json.getJsonArray("targets").map { ConnectionSource(it as JsonObject) },
    json.getJsonObject("data")
  )
}

data class RabbitMQBrokerConfig(val uri: String, val outExchange: String, val inExchange: String)
