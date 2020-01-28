package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.models.ConnectionSource
import com.github.danbrato999.brokerws.models.WebSocketConnection
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

internal class WebSocketServerStoreImplTest {

  private lateinit var store: WebSocketServerStoreImpl

  @BeforeEach
  fun init() {
    store = WebSocketServerStoreImpl()
  }

  @Test
  fun testAddNewConnection() {
    val ws = mock(ServerWebSocket::class.java)

    val connection = WebSocketConnection(ConnectionSource("junit", "test001"), ws)
    assertTrue(store.store(connection), "Storing a new connection shouldn't override anything")

    val connection2 = WebSocketConnection(ConnectionSource("junit", "test002"), ws)
    assertTrue(store.store(connection2), "Storing a new connection shouldn't override anything")

    val connection3 = WebSocketConnection(ConnectionSource("junit", "test001"), ws)
    assertFalse(store.store(connection3), "Duplicated connections should be overridden")
  }

  @Test
  fun testBroadcast() {
    val message = JsonObject().put("message", "Hello world!!")

    val ws1 = mock(ServerWebSocket::class.java)
    val ws2 = mock(ServerWebSocket::class.java)
    val connection = WebSocketConnection(ConnectionSource("junit", "test001"), ws1)
    val connection2 = WebSocketConnection(ConnectionSource("junit", "test002"), ws2)
    store.store(connection)
    store.store(connection2)


    store.broadcast(listOf(connection.source), message)
    store.broadcast(listOf(connection.source, connection2.source), message)

    verify(ws1, times(2)).writeTextMessage(message.encode())
    verify(ws2, times(1)).writeTextMessage(message.encode())
  }
}
