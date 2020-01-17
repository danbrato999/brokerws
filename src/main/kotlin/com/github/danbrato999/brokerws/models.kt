package com.github.danbrato999.brokerws

import io.vertx.core.json.JsonObject

data class OutgoingMessage(val targets: List<String>, val data: JsonObject) {
  constructor(json: JsonObject) : this(
    json.getJsonArray("targets").map { it.toString() },
    json.getJsonObject("data")
  )
}
