package com.github.danbrato999.brokerws.models

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject
import java.util.*

@DataObject
data class ConnectionSource(
  val requestId: String = "",
  val entityId: String,
  val serverId: String = "",
  val details: JsonObject = JsonObject()
) {
  constructor(json: JsonObject) : this(
    json.getString("requestId", ""),
    json.getString("entityId"),
    json.getString("serverId", ""),
    json.getJsonObject("details", JsonObject())
  )

  fun toJson() : JsonObject =
      JsonObject.mapFrom(this)
}
