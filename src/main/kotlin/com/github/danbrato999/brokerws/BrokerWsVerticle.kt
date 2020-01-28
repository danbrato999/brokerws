package com.github.danbrato999.brokerws

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.RabbitMQBrokerConfig
import com.github.danbrato999.brokerws.services.BrokerWsHandler
import com.github.danbrato999.brokerws.services.impl.WebSocketServerStoreImpl
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer

class BrokerWsVerticle : AbstractVerticle() {
  private val uriRegex = Regex("^/ws/(.*)/(.*)$")

  override fun start(startPromise: Promise<Void>) {
    val config = config().getJsonObject("rabbitmq")
    val uri = config.getString("uri")
    val outMessageExchange = config.getJsonObject("out").getString("exchange")
    val inMessageExchange = config.getJsonObject("in").getString("exchange")

    val rabbitMQBrokerConfig = RabbitMQBrokerConfig(uri, outMessageExchange, inMessageExchange)
    val store = WebSocketServerStoreImpl()

    Future.future<BrokerWsHandler> { BrokerWsHandler.create(vertx, store, rabbitMQBrokerConfig, it) }
      .compose { wsHandler ->
        Future.future<HttpServer> {
          vertx.createHttpServer()
            .requestHandler { request ->
              if (uriRegex matches request.uri()) {
                try {
                  val matches = uriRegex.find(request.uri())!!
                    .groupValues
                    .drop(1)
                  val source = ConnectionSource(matches.first(), matches.last())
                  wsHandler.handle(source, request)
                } catch (e: Exception) {
                  request
                    .response()
                    .setStatusCode(500)
                    .end()
                }
              }
            }
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
