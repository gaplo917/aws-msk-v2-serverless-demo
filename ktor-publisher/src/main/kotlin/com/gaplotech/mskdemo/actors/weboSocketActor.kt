package com.gaplotech.mskdemo.actors

import com.gaplotech.mskdemo.extensions.toJsonString
import com.google.protobuf.Message
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

sealed class WebSocketActorEnvelop<T>(open val message: T)

class ProtobufMessageWSAEnvelop(override val message: Message): WebSocketActorEnvelop<Message>(message)

class AddSessionWSAEnvelop(override val message: DefaultWebSocketSession): WebSocketActorEnvelop<DefaultWebSocketSession>(message)

class RemoveSessionWSAEnvelop(override val message: DefaultWebSocketSession): WebSocketActorEnvelop<DefaultWebSocketSession>(message)

fun CoroutineScope.websocketActor(): SendChannel<WebSocketActorEnvelop<*>> = Channel<WebSocketActorEnvelop<*>>().also { channel ->
    launch(Dispatchers.Default) {
        val connections = linkedSetOf<DefaultWebSocketSession>()
        var count = 0
        for (envelop in channel) { // iterate over incoming messages
            when (envelop) {
                is ProtobufMessageWSAEnvelop -> {
                    connections.forEach { session ->
                        session.send(Frame.Text(envelop.message.toJsonString()))
                    }
                }
                is AddSessionWSAEnvelop -> {
                    val newSession = envelop.message
                    connections += newSession
                    count++
                    newSession.send(Frame.Text("welcome, your are the $count user joined this session!"))
                }
                is RemoveSessionWSAEnvelop -> {
                    connections.remove(envelop.message)
                }
            }

        }
    }
}