package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.services.WebSocketBaseStore
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.deliveryOptionsOf
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class WebSocketBaseStoreProxy(
  private val vertx: Vertx,
  private val serviceAddress: String
) : WebSocketBaseStore {
  override fun broadcast(targets: List<ConnectionSource>, message: JsonObject): WebSocketBaseStore {
    val options = deliveryOptionsOf(
      headers = mapOf("action" to "broadcast")
    )
    val body = json {
      obj(
        "targets" to array(targets.map { it.toJson() }),
        "message" to message
      )
    }

    vertx.eventBus()
      .publish(serviceAddress, body, options)

    return this
  }

  override fun delete(source: ConnectionSource, handler: Handler<AsyncResult<Boolean>>): WebSocketBaseStore {
    val options = deliveryOptionsOf(
      headers = mapOf("action" to "delete")
    )
    val body = json {
      obj(
        "source" to source.toJson()
      )
    }

    vertx.eventBus()
      .publish(serviceAddress, body, options)

    // This is fake, just for testing purposes
    handler.handle(Future.succeededFuture(true))
    return this
  }
}
