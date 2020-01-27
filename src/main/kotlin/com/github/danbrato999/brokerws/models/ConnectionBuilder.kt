package com.github.danbrato999.brokerws.models

import io.vertx.core.Handler
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class ConnectionBuilder {
  private lateinit var source: ConnectionSource
  private lateinit var connection: ServerWebSocket
  private lateinit var messageHandler: Handler<String>
  private var closeHandler: Handler<Void>? = null

  fun withSource(source: ConnectionSource) : ConnectionBuilder = apply {
    this.source = source
  }

  fun withConnection(connection: ServerWebSocket) : ConnectionBuilder = apply {
    this.connection = connection
  }

  fun withMessageHandler(f: (JsonObject) -> Unit) : ConnectionBuilder = apply {
    this.messageHandler = Handler { msg ->
      val detailedMessage = json {
        obj(
          "source" to JsonObject.mapFrom(source),
          "data" to msg
        )
      }

      f(detailedMessage)
    }
  }

  fun withCloseHandler(f: (ConnectionSource) -> Unit) : ConnectionBuilder = apply {
    this.closeHandler = Handler { f(source) }
  }

  fun build() : WebSocketConnection {
    connection.textMessageHandler(messageHandler)
    if (closeHandler != null)
      connection.closeHandler(closeHandler)

    return WebSocketConnection(source, connection)
  }
}
