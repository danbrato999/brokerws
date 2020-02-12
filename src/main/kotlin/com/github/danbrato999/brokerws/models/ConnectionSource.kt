package com.github.danbrato999.brokerws.models

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject
import java.util.*

@DataObject
data class ConnectionSource(
  val requestId: String = "",
  val entityId: String,
  val serverId: String = ""
) {
  constructor(json: JsonObject) : this(
    json.getString("requestId", ""),
    json.getString("entityId"),
    json.getString("serverId", "")
  )

  fun toJson() : JsonObject =
      JsonObject.mapFrom(this)
}
