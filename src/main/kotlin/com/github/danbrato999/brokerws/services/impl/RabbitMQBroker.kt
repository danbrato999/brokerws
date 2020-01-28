package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.OutgoingMessage
import com.github.danbrato999.brokerws.models.RabbitMQBrokerConfig
import com.github.danbrato999.brokerws.services.WebSocketBaseStore
import com.github.danbrato999.brokerws.services.WebSocketBroker
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.rabbitmq.rabbitMQOptionsOf
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQConsumer

class RabbitMQBroker private constructor(
  private val wsStore: WebSocketBaseStore,
  private val client: RabbitMQClient,
  private val forwardExchange: String
) : WebSocketBroker {

  override fun notifyNewConnection(source: ConnectionSource): WebSocketBroker {
    return this
  }

  override fun receiveMessage(message: JsonObject): WebSocketBroker {
    Logger.debug("Forwarding new incoming message -> $message")
    client.basicPublish(forwardExchange, "", JsonObject().put("body", message.encode())) { ar ->
      if (ar.failed())
        Logger.error("Failed to publish message to RabbitMQ", ar.cause())
    }

    return this
  }

  override fun notifyConnectionClosed(source: ConnectionSource): WebSocketBroker {
    return this
  }

  private fun withMessageConsumer(queue: String) : Future<RabbitMQBroker> = Future.future<RabbitMQConsumer> {
    client.basicConsumer(queue, it)
  }.map { consumer ->
    consumer.handler {
      Logger.debug("Received new outgoing message from queue -> ${it.body()}")
      val message = OutgoingMessage(it.body().toJsonObject())
      wsStore.broadcast(message.targets, message.data)
    }

    this
  }

  companion object {
    private val Logger = LoggerFactory.getLogger(WebSocketBroker::class.java)

    fun create(
      vertx: Vertx,
      wsStore: WebSocketBaseStore,
      config: RabbitMQBrokerConfig,
      handler: Handler<AsyncResult<WebSocketBroker>>
    ) {
      val client = RabbitMQClient.create(vertx, rabbitMQOptionsOf(uri = config.uri))

      Future.future<Void> {
        client.start(it)
      }
        .compose {
          initFanOutExchanges(
            client,
            listOf(config.inExchange, config.outExchange)
          )
        }.compose {
          bindQueue(
            client,
            config.outExchange
          )
        }.compose { messageQueue ->
          RabbitMQBroker(
            wsStore,
            client,
            config.inExchange
          )
            .withMessageConsumer(messageQueue)
        }.setHandler { ar ->
          handler.handle(ar.map { it as WebSocketBroker })
        }
    }

    private fun initFanOutExchanges(client: RabbitMQClient, exchanges: List<String>) = exchanges
      .map { exchange ->
        Future.future<Void> {
          client.exchangeDeclare(exchange, "fanout", true, false, it)
        }
      }.let(CompositeFuture::join)

    private fun bindQueue(client: RabbitMQClient, exchange: String) : Future<String> = Future.future<JsonObject> {
      client.queueDeclare("", false, true, true, it)
    }.compose {
      val queue = it.getString("queue")

      Future.future<Void> { promise ->
        client.queueBind(queue, exchange, "", promise)
      }.map {
        queue
      }
    }
  }
}
