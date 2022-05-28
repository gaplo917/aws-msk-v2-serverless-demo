package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.actors.AddSessionWSAEnvelop
import com.gaplotech.mskdemo.actors.RemoveSessionWSAEnvelop
import com.gaplotech.mskdemo.actors.WebSocketActorEnvelop
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.SendChannel

fun Application.configureWebSocket(
    webSocketActor: SendChannel<WebSocketActorEnvelop<*>>
) {
    install(WebSockets)

    routing {
        webSocket("/subscription") {
            // Handle a WebSocket session
            webSocketActor.send(AddSessionWSAEnvelop(this))
            for (frame in incoming) {
                // do nothing
            }
            webSocketActor.send(RemoveSessionWSAEnvelop(this))
        }
    }

}