package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.*
import com.github.danbrato999.brokerws.services.WebSocketBaseStore
import com.github.danbrato999.brokerws.services.WebSocketBroker
import com.github.danbrato999.brokerws.utils.RabbitMQStarter
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.rabbitmq.rabbitMQOptionsOf
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQConsumer
import org.slf4j.LoggerFactory

class RabbitMQBroker(
  private val wsStore: WebSocketBaseStore,
  private val config: RabbitMQBrokerConfig
) : WebSocketBroker {
  private lateinit var client: RabbitMQClient

  override fun notifyNewConnection(source: ConnectionSource): WebSocketBroker {
    val event = BrokerWsConnectionEvent(WsConnectionEventType.Connection, source)
    client.basicPublish(config.events, "", event.toRabbitMQ()) {
      if (it.failed())
        Logger.error("Failed to notify new connection", it.cause())
    }

    return this
  }

  override fun receiveMessage(message: JsonObject): WebSocketBroker {
    Logger.debug("Forwarding new incoming message -> $message")
    client.basicPublish(config.incomingMessages, "", JsonObject().put("body", message.encode())) { ar ->
      if (ar.failed())
        Logger.error("Failed to publish message to RabbitMQ", ar.cause())
    }

    return this
  }

  override fun notifyConnectionClosed(source: ConnectionSource): WebSocketBroker {
    val event = BrokerWsConnectionEvent(WsConnectionEventType.Disconnection, source)
    client.basicPublish(config.events, "", event.toRabbitMQ()) {
      if (it.failed())
        Logger.error("Failed to notify connection closed", it.cause())
    }

    return this
  }

  fun start(vertx: Vertx, handler: Handler<AsyncResult<WebSocketBroker>>) {
    client = RabbitMQClient.create(vertx, rabbitMQOptionsOf(uri = config.uri))

    Future.future<Void> { client.start(it) }
      .compose {
        RabbitMQStarter.initFanOutExchanges(client, listOf(config.incomingMessages, config.events))
      }
      .compose {
        RabbitMQStarter.initMessageConfig(client, config.outgoingMessages)
      }
      .compose {
        withMessageConsumer(it.queue)
      }
      .map { this as WebSocketBroker }
      .setHandler(handler)
  }

  private fun withMessageConsumer(queue: String) = Future.future<RabbitMQConsumer> {
    client.basicConsumer(queue, it)
  }.map { consumer ->
    consumer.handler {
      Logger.debug("Received new WebSocket outgoing message -> ${it.body()}")
      val message = OutgoingMessage(it.body().toJsonObject())
      wsStore.broadcast(message.targets, message.data)
    }
  }

  companion object {
    private val Logger = LoggerFactory.getLogger(WebSocketBroker::class.java)

    fun BrokerWsConnectionEvent.toRabbitMQ() : JsonObject = JsonObject()
      .put("body", this.toJson().encode())
  }
}
