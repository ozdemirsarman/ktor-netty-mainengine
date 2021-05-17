package com.example.plugins



import kotlin.math.sin
import kotlin.random.Random

import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.sessions.*
import org.slf4j.event.Level
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*

import java.io.*
import java.util.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.util.*
import kotlin.coroutines.*
import kotlinx.coroutines.*
import io.ktor.utils.io.*

import io.ktor.client.*

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*



fun Application.configureRouting() {



    install(Authentication) {
        basic("myBasicAuth") {
            realm = "Ktor Server"
            validate { if (it.name == "test" && it.password == "password") UserIdPrincipal(it.name) else null }
        }
    }

    install(ContentNegotiation) {
        gson {
        }
    }

 install(Sessions) {
       cookie<MySession>("MY_SESSION") {
           cookie.extensions["SameSite"] = "lax"
        }
    }



    install(CallLogging) {
        level = Level.TRACE
        filter { call -> call.request.path().startsWith("/") }
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }


    HttpClient() {
        engine {
            // this: HttpClientEngineConfig
            threadsCount = 4
            pipelining = true
        }
    }



    routing {

        get("/listing") {
            call.respondText("Listing items")
        }

        get("/random") {

            val randomValues = List(10) { Random.nextInt(0, 100) }
           // call.respondText("Random strings")
            call.respondText(text = "$randomValues")
        }

        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        authenticate("myBasicAuth") {
            get("/protected/route/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }

        get("/session/increment") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(count = session.count + 1))
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }

        webSocket("/myws/echo") {
            send(Frame.Text("Hi from server"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    send(Frame.Text("Client said: " + frame.readText()))
                }
            }
        }


    }
}


data class MySession(val count: Int = 0)
