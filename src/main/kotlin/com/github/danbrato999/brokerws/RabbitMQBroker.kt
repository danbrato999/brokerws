package com.github.danbrato999.brokerws

import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.rabbitmq.rabbitMQOptionsOf
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQConsumer
import org.slf4j.LoggerFactory

class RabbitMQBroker : AbstractVerticle() {
  override fun start(startPromise: Promise<Void>) {
    val config = config().getJsonObject("rabbitmq")
    val uri = config.getString("uri")
    val outMessageExchange = config.getJsonObject("out").getString("exchange")
    val inMessageExchange = config.getJsonObject("in").getString("exchange")

    val client = RabbitMQClient.create(vertx, rabbitMQOptionsOf(uri = uri))
    Future.future<Void> { client.start(it) }
      .compose {
        initFanOutExchanges(client, listOf(inMessageExchange, outMessageExchange))
      }
      .compose {
        forwardIncomingMessages(client, inMessageExchange)

        bindQueue(client, outMessageExchange)
          .compose { queue -> createOutgoingConsumer(client, queue) }
      }.setHandler { ar ->
        if (ar.succeeded())
          startPromise.complete()
        else
          startPromise.fail(ar.cause())
      }
  }

  private fun createOutgoingConsumer(client: RabbitMQClient, queue: String) = Future.future<RabbitMQConsumer> {
    client.basicConsumer(queue, it)
  }.map { consumer ->
    consumer.handler {
      vertx.eventBus().publish(OUTGOING_MESSAGES_ADDRESS, it.body().toJsonObject())
    }
  }

  private fun forwardIncomingMessages(client: RabbitMQClient, exchange: String) {
    vertx.eventBus().consumer<JsonObject>(INCOMING_MESSAGES_ADDRESS) {
      val message = JsonObject().put("body", it.body().encode())
      client.basicPublish(exchange, "", message) { ar ->
        if (ar.failed())
          Logger.error("Failed to publish message to RabbitMQ", ar.cause())
      }
    }
  }

  companion object {
    private val Logger = LoggerFactory.getLogger(RabbitMQBroker::class.java)

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
