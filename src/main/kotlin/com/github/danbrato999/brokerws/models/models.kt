package com.github.danbrato999.brokerws.models

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@DataObject
data class ConnectionSource(val entity: String, val id: String) {
  constructor(json: JsonObject) : this(json.getString("entity"), json.getString("id"))
  fun toJson() : JsonObject = JsonObject.mapFrom(this)
}

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

/**
 * This is the required configuration for a RabbitMQ broker. It publishes events and messages from the BrokerWS
 * server and consumes the messages that need to be send to the WebSocket connections
 */
data class RabbitMQBrokerConfig(
  /**
   * RabbitMQ server uri
   */
  val uri: String,
  /**
   * Source of messages to be send to the WebSocket connections
   */
  val outgoingMessages: RabbitMQExchangeQueueConfig,
  /**
   * Exchange to send the received WebSocket messages
   */
  val incomingMessages: String,
  /**
   * Exchange to send connection events to
   * TODO Make it a full duplex channel between broker and worker
   */
  val events: String
) {
  constructor(json: JsonObject) : this(
    json.getString("uri"),
    RabbitMQExchangeQueueConfig(json.getJsonObject("outgoingMessages")),
    json.getString("incomingMessages"),
    json.getString("events")
  )
}

/**
 * This is the required configuration for a RabbitMQ worker. The worker serves as the opposite of a Broker, meaning
 * it consumes messages and events from the BrokerWS server and publishes messages to be send to the WebSocket connections
 */
data class RabbitMQWorkerConfig(
  val uri: String,
  /**
   * Exchange to send messages to the WebSocket
   */
  val outgoingMessages: String,
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
    json.getString("outgoingMessages"),
    RabbitMQExchangeQueueConfig(json.getJsonObject("incomingMessages")),
    RabbitMQExchangeQueueConfig(json.getJsonObject("events"))
  )
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
