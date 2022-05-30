package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.actors.AddWSSessionEnvelop
import com.gaplotech.mskdemo.actors.RemoveWSSessionEnvelop
import com.gaplotech.mskdemo.actors.WebSocketActorEnvelop
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.SendChannel
import java.util.*

fun Application.configureWebSocket(
    webSocketActor: SendChannel<WebSocketActorEnvelop<*>>
) {
    install(WebSockets)

    routing {
        webSocket("/subscription") {
            val clientId = UUID.randomUUID()
            // Handle a WebSocket session
            webSocketActor.send(AddWSSessionEnvelop(this, clientId))
            for (frame in incoming) {
                // do nothing
            }
            // when client disconnect, the coroutine will be ended
            // then we remove the session in the actor
            webSocketActor.send(RemoveWSSessionEnvelop(this, clientId))
        }
    }

}