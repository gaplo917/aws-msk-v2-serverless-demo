package com.gaplotech.mskdemo.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    // Starting point for a Ktor app:
    routing {
        get("/ping") {
            call.respondText("Ok")
        }
    }
}
