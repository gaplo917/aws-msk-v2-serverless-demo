package com.gaplotech.mskdemo.actors

import com.gaplotech.mskdemo.extensions.toJsonString
import com.google.protobuf.Message
import io.ktor.server.application.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.*

sealed class WebSocketActorEnvelop<T>(open val message: T)

class BroadcastProtobufMessageEnvelop(override val message: Message) : WebSocketActorEnvelop<Message>(message)

class AddWSSessionEnvelop(override val message: DefaultWebSocketSession, val clientId: UUID) :
    WebSocketActorEnvelop<DefaultWebSocketSession>(message)

class RemoveWSSessionEnvelop(override val message: DefaultWebSocketSession, val clientId: UUID) :
    WebSocketActorEnvelop<DefaultWebSocketSession>(message)

fun Application.websocketActor(): SendChannel<WebSocketActorEnvelop<*>> =
    Channel<WebSocketActorEnvelop<*>>().also { channel ->
        val logger = log
        launch(Dispatchers.Default) {
            val connections = linkedSetOf<DefaultWebSocketSession>()
            var count = 0
            for (envelop in channel) { // iterate over incoming messages
                when (envelop) {
                    is BroadcastProtobufMessageEnvelop -> {
                        connections.forEach { session ->
                            session.send(Frame.Text(envelop.message.toJsonString()))
                        }
                    }
                    is AddWSSessionEnvelop -> {
                        val newSession = envelop.message
                        connections += newSession
                        count++
                        logger.info("added a new websocket client id={}, totalCount={}", envelop.clientId, count)
                        newSession.send(Frame.Text("welcome, your client id is ${envelop.clientId}!"))
                    }
                    is RemoveWSSessionEnvelop -> {
                        logger.info("removed a websocket client id={}, totalCount={}", envelop.clientId, count)
                        connections.remove(envelop.message)
                    }
                }

            }
        }
    }