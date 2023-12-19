package at.privatepilot.server


import android.content.Context
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Http(private val port: Int, val encryption: Encryption, val context: Context) {
    init {
        val userAuth = UserAuth(context)
        // Start the Ktor server in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            // Start the Ktor server
            embeddedServer(Netty, port) {
                install(ContentNegotiation) {
                    gson {}
                }
                routing {
                    get("/") {
                        call.respond(mapOf("message" to "Hello world"))
                    }
                    get("/public-key") {
                        call.respondText(encryption.getPublicKey())
                    }
                    post("/register-user") {
                        val name = call.request.headers["name"] ?: ""
                        val data = call.receiveText()

                        try {
                            userAuth.registerUser(name, data)
                            call.respondText("User registered successfully")
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Bad Request")
                        }
                    }
                }
            }.start(wait = true)
        }
    }
}
