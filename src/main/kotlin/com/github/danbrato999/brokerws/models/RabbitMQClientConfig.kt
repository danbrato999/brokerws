package com.github.danbrato999.brokerws.models

import io.vertx.core.json.JsonObject

data class RabbitMQClientConfig(
  val uri: String,
  /**
   * Exchange to send messages to the WebSocket
   */
  val outgoingMessages: RabbitMQExchangeQueueConfig,
  /**
   * Source of messages to process
   */
  val incomingMessages: RabbitMQExchangeQueueConfig,
  /**
   * Source of events to react to
   */
  val events: RabbitMQExchangeQueueConfig
) {
  constructor(json: JsonObject) : this(
    json.getString("uri"),
    RabbitMQExchangeQueueConfig(json.getString("outgoingMessages")),
    RabbitMQExchangeQueueConfig(json.getJsonObject("incomingMessages")),
    RabbitMQExchangeQueueConfig(json.getJsonObject("events"))
  )
}
