package com.github.danbrato999.brokerws

import com.github.danbrato999.brokerws.models.RabbitMQBrokerConfig
import com.github.danbrato999.brokerws.services.BrokerWsServer
import com.github.danbrato999.brokerws.services.impl.WebSocketServerStoreImpl
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer

class BrokerWsVerticle : AbstractVerticle() {
  override fun start(startPromise: Promise<Void>) {
    val config = config().getJsonObject("rabbitmq")
    val uri = config.getString("uri")
    val outMessageExchange = config.getJsonObject("out").getString("exchange")
    val inMessageExchange = config.getJsonObject("in").getString("exchange")

    val rabbitMQBrokerConfig = RabbitMQBrokerConfig(uri, outMessageExchange, inMessageExchange)
    val store = WebSocketServerStoreImpl()

    Future.future<BrokerWsServer> { BrokerWsServer.create(vertx, store, rabbitMQBrokerConfig, it) }
      .map { it.router() }
      .compose { router ->
        Future.future<HttpServer> {
          vertx.createHttpServer()
            .requestHandler(router)
            .listen(config().getString("port").toInt(), it)
        }
      }
      .setHandler {
        if (it.succeeded())
          startPromise.complete()
        else {
          it.cause().printStackTrace()
          startPromise.fail(it.cause())
        }
      }
  }
}
