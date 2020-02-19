package com.github.danbrato999.brokerws.services.client

import com.github.danbrato999.brokerws.models.*
import com.github.danbrato999.brokerws.utils.RabbitMQStarter
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.rabbitmq.rabbitMQOptionsOf
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQConsumer
import org.slf4j.LoggerFactory

class RabbitMQWorker(
  private val config: RabbitMQClientConfig
) : BrokerWsWorker {
  private lateinit var client: RabbitMQClient

  override fun sendMessage(source: ConnectionSource, message: JsonObject) {
    val outgoingMessage = OutgoingMessage(listOf(source), message)
      .toJson()
      .encode()

    client.basicPublish(config.outgoingMessages.exchange, "", JsonObject().put("body", outgoingMessage)) {
      if (it.failed())
        Logger.error("Failed to send message to BrokerWS", it.cause())
    }
  }

  override fun broadcast(connections: List<ConnectionSource>, message: JsonObject) {
    val outgoingMessage = OutgoingMessage(connections, message)
      .toJson()
      .encode()

    client.basicPublish(config.outgoingMessages.exchange, "", JsonObject().put("body", outgoingMessage)) {
      if (it.failed())
        Logger.error("Failed to broadcast message to BrokerWS", it.cause())
    }
  }

  override fun closeConnections(connections: List<ConnectionSource>, message: JsonObject) {
    val event = BrokerWsConnectionEvent(
      WsConnectionEventType.Replaced,
      connections,
      message
    )
      .toJson()
      .encode()

    client.basicPublish(config.events.exchange, "", JsonObject().put("body", event)) {
      if (it.failed())
        Logger.error("Error to send ws connection closing request", it.cause())
    }
  }

  fun start(vertx: Vertx, handler: Handler<AsyncResult<RabbitMQWorker>>) {
    client = RabbitMQClient.create(vertx, rabbitMQOptionsOf(uri = config.uri))

    Future.future<Void> { client.start(it) }
      .compose {
        RabbitMQStarter.initFanOutExchanges(client, listOf(config.outgoingMessages.exchange))
      }
      .compose {
        RabbitMQStarter.initMessageConfig(client, config.incomingMessages)
      }
      .map { this }
      .setHandler(handler)
  }

  fun withEventHandler(handler: BrokerWsEventHandler): Future<RabbitMQWorker> =
    RabbitMQStarter.initMessageConfig(client, config.events)
      .compose { conf ->
        Future.future<RabbitMQConsumer> {
          client.basicConsumer(conf.queue, it)
        }
      }
      .map { consumer ->
        consumer.handler { msg ->
          val event = BrokerWsConnectionEvent(msg.body().toJsonObject())

          when (event.type) {
            WsConnectionEventType.Connection -> event.connections.forEach { handler.handleNewConnection(it) }
            WsConnectionEventType.Disconnection -> event.connections.forEach { handler.handleDisconnection(it) }
            else -> Unit
          }
        }

        this
      }

  fun withMessageHandler(handler: BrokerWsMessageHandler): Future<RabbitMQWorker> =
    RabbitMQStarter.initMessageConfig(client, config.incomingMessages)
      .compose { conf ->
        Future.future<RabbitMQConsumer> {
          client.basicConsumer(conf.queue, it)
        }
      }.map { consumer ->
        consumer.handler {
          val incomingMessage = IncomingMessage(it.body().toJsonObject())
          handler.handle(incomingMessage)
        }

        this
      }

  companion object {
    private val Logger = LoggerFactory.getLogger(RabbitMQWorker::class.java)
  }
}
