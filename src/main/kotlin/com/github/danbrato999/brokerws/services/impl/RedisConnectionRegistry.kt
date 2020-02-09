package com.github.danbrato999.brokerws.services.impl

import com.github.danbrato999.brokerws.services.ConnectionRegistry
import com.github.danbrato999.brokerws.models.ConnectionSource
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.Response

class RedisConnectionRegistry(
  private val redisApi: RedisAPI
) : ConnectionRegistry {
  override fun add(source: ConnectionSource, handler: Handler<AsyncResult<String>>): ConnectionRegistry {
    val entityKey = entityKey(source.entityId)
    val byEntityStore = getRequestsByEntity(entityKey)
      .compose { requests ->
        Future.future<Response> {
          redisApi.set(listOf(entityKey, requests.add(source.requestId).toString()), it)
        }
      }

    val connStoreFuture = Future.future<Response> {
      redisApi.set(listOf(mapKey(source.requestId), source.toJson().encode()), it)
    }

    CompositeFuture.all(connStoreFuture, byEntityStore)
      .map { source.requestId }
      .setHandler(handler)

    return this
  }

  override fun findByRequestId(
    requestId: String,
    handler: Handler<AsyncResult<ConnectionSource?>>
  ): ConnectionRegistry {
    redisApi.get(mapKey(requestId)) { ar ->
      handler.handle(
        ar.map { response ->
          response?.let {
            ConnectionSource(JsonObject(it.toString()))
          }
        }
      )
    }

    return this
  }

  override fun findByEntityId(
    entityId: String,
    handler: Handler<AsyncResult<List<ConnectionSource>>>
  ): ConnectionRegistry {
    getRequestsByEntity(entityKey(entityId))
      .compose { requests ->
        Future.future<Response> { promise ->
          redisApi.mget(requests.map { it as String }, promise)
        }
      }
      .map { list ->
        list.map {
          ConnectionSource(JsonObject(it.toString()))
        }
      }
      .setHandler(handler)

    return this
  }

  override fun delete(source: ConnectionSource, handler: Handler<AsyncResult<String>>): ConnectionRegistry {
    redisApi.del(listOf(mapKey(source.requestId))) { ar ->
      handler.handle(ar.map(source.requestId))
    }

    return this
  }

  private fun getRequestsByEntity(entityKey: String) : Future<JsonArray> = Future.future<Response?>{
    redisApi.get(entityKey, it)
  }.map { response ->
    response
      ?.toString()
      ?.let { JsonArray(it) } ?: JsonArray()
  }

  companion object {
    private const val PREFIX = "brokerws.registry."
    private fun mapKey(requestId: String): String = PREFIX + "source." + requestId
    private fun entityKey(entityId: String): String = PREFIX + "entity." + entityId
  }
}
