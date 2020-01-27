package com.github.danbrato999.brokerws.models

import io.vertx.core.http.ServerWebSocket
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

internal class ConnectionBuilderTest {

  @Test
  fun wrongBuild() {
    val emptyBuilder = ConnectionBuilder()
    assertThrows(UninitializedPropertyAccessException::class.java, { emptyBuilder.build() }, "Builder created empty connection")

    val missingConnection = ConnectionBuilder()
      .withSource(SOURCE)
      .withMessageHandler {  }

    assertThrows(UninitializedPropertyAccessException::class.java, { missingConnection.build() }, "Builder created with missing connection")

    val missingSource = ConnectionBuilder()
      .withConnection(mock(ServerWebSocket::class.java))
      .withMessageHandler {  }

    assertThrows(UninitializedPropertyAccessException::class.java, { missingSource.build() }, "Builder created without source")

    val missingMessageHandler = ConnectionBuilder()
      .withConnection(mock(ServerWebSocket::class.java))
      .withSource(SOURCE)

    assertThrows(UninitializedPropertyAccessException::class.java, { missingMessageHandler.build() }, "Builder created without message handler")
  }

  @Test
  fun validBuild() {
    val withoutCloseHandler = ConnectionBuilder()
      .withSource(SOURCE)
      .withConnection(mock(ServerWebSocket::class.java))
      .withMessageHandler {  }

    assertDoesNotThrow { withoutCloseHandler.build() }

    val withCloseHandler = ConnectionBuilder()
      .withSource(SOURCE)
      .withConnection(mock(ServerWebSocket::class.java))
      .withMessageHandler {  }
      .withCloseHandler {  }

    assertDoesNotThrow { withCloseHandler.build() }
  }

  companion object {
    private val SOURCE = ConnectionSource("entity", "id")
  }
}
