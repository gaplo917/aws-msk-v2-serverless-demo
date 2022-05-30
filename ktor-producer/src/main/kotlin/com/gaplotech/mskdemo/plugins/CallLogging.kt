package com.gaplotech.mskdemo.plugins

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import org.slf4j.event.Level

fun Application.configureCallLogging() {
    install(CallLogging) {
        val config = ConfigFactory.load()
        level = Level.valueOf(config.getString("ktor.logging.level"))
    }
}