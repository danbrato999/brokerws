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
    val byEntityStore = addRequestToEntity(source.entityId, source.requestId)

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
    getRequestsByEntity(entityId)
      .compose<List<ConnectionSource>> { requests ->
        val requestKeys = requests
          .map { mapKey(it) }

        if (requestKeys.isEmpty())
          Future.succeededFuture(listOf())
        else
          Future.future<Response> { promise -> redisApi.mget(requestKeys, promise) }
            .map { list ->
              list
                .filterNotNull()
                .map { ConnectionSource(JsonObject(it.toString())) }
            }
      }
      .setHandler(handler)

    return this
  }

  override fun delete(source: ConnectionSource, handler: Handler<AsyncResult<String>>): ConnectionRegistry {
    Future.future<Response> {
      redisApi.del(listOf(mapKey(source.requestId)), it)
    }
      .compose {
        getRequestsByEntity(source.entityId)
      }
      .compose { array ->
        val currentRequests = array
          .filter { it != source.requestId }

        Future.future<Response> {
          val entityKey = entityKey(source.entityId)
          if (currentRequests.isEmpty())
            redisApi.del(listOf(entityKey), it)
          else
            redisApi.set(listOf(entityKey, JsonArray(currentRequests).toString()), it)
        }
      }
      .map { source.requestId }
      .setHandler(handler)

    return this
  }

  private fun getRequestsByEntity(entityId: String) : Future<List<String>> = Future.future<Response?>{
    redisApi.get(entityKey(entityId), it)
  }.map { response ->
    response
      ?.let {
        JsonArray(it.toBuffer())
      }
      ?.map { it.toString() } ?: emptyList()
  }

  private fun addRequestToEntity(entityId: String, newRequest: String) =
    getRequestsByEntity(entityId)
      .compose { requests ->
        val keyValue = JsonArray(requests.plus(newRequest))
          .encode()
        Future.future<Response> {
          redisApi.set(listOf(entityKey(entityId), keyValue), it)
        }
      }

  companion object {
    private const val PREFIX = "brokerws.registry."
    private fun mapKey(requestId: String): String = PREFIX + "source." + requestId
    private fun entityKey(entityId: String): String = PREFIX + "entity." + entityId
  }
}
