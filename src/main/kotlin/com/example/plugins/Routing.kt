package com.example.plugins


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
import io.ktor.client.*
import io.ktor.thymeleaf.*
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.util.*
import io.ktor.html.*
import io.ktor.http.content.*
import kotlinx.html.*



fun Application.configureRouting() {

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }


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

        static("/static") {
            resources("files")
        }


        get("/index") {

            val dateNow = Calendar.getInstance().time

            val sampleUser = User(1, "John")

            call.respond(ThymeleafContent("index", mapOf("user" to sampleUser)))

        }

        get("/homepage") {
            call.respondText("<br><center><h1>Home Page</center></h1>", ContentType.Text.Html, HttpStatusCode.OK)
        }

        get("/homepage2") {
            val name = "Ktor"
            val dateNow = Calendar.getInstance().time
            call.respondHtml {
                head {
                    title {
                        +name
                    }
                }
                body {
                    h1 {
                        +"Hello from $name!"
                    }

                    h1 {

                        +"Date is $dateNow"

                    }
                }
            }
        }


        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }



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

        get("/date") {

            val dateNow = Calendar.getInstance().time

            call.respondText("$dateNow", contentType = ContentType.Text.Plain)
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
data class User(val id: Int, val name: String)
