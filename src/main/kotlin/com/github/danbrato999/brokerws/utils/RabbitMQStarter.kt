package com.github.danbrato999.brokerws.utils

import com.github.danbrato999.brokerws.models.RabbitMQExchangeQueueConfig
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.rabbitmq.RabbitMQClient

object RabbitMQStarter {
  fun initFanOutExchanges(client: RabbitMQClient, exchanges: List<String>) : CompositeFuture = exchanges
    .map { exchange ->
      Future.future<Void> {
        client.exchangeDeclare(exchange, "fanout", true, false, it)
      }
    }.let(CompositeFuture::join)

  fun initMessageConfig(
    client: RabbitMQClient,
    config: RabbitMQExchangeQueueConfig
  ) : Future<RabbitMQExchangeQueueConfig> =
    Future.future<Void> {
      client.exchangeDeclare(config.exchange, config.exchangeType, true, false, it)
    }
      .compose {
        Future.future<JsonObject> {
          client.queueDeclare(config.queue, config.durable, true, true, it)
        }
      }
      .compose {
        val queue = it.getString("queue")

        Future.future<Void> { promise ->
          client.queueBind(queue, config.exchange, "", promise)
        }.map {
          config.copy(queue = queue)
        }
      }
}
