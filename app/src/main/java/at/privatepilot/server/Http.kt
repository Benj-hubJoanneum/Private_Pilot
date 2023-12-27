package at.privatepilot.server

import android.content.Context
import io.ktor.application.call
import io.ktor.application.install
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
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Http(private val keyPort: Int, private val registerPort: Int, val encryption: Encryption, val context: Context) {

    private var registerServer: NettyApplicationEngine? = null
    private val userAuth = UserAuth(context)

    init {
        // Start the Ktor server for /public-key in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            embeddedServer(Netty, keyPort) {
                install(ContentNegotiation) {
                    gson {}
                }
                routing {
                    get("/public-key") {
                        call.respondText(encryption.getPublicKey())
                    }
                    get("/ip") {
                        call.respondText(NetworkManager.getInternetIpAddress())
                    }
                }
            }.start(wait = true)
        }
    }

    fun startRegisterUserServer(){
        // Start the Ktor server for /register-user in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            registerServer = embeddedServer(Netty, registerPort) {
                install(ContentNegotiation) {
                    gson {}
                }
                routing {
                    get("/") {
                        call.respond("Attention register Server is Open!")
                    }
                    post("/register-user") {
                        val name = call.request.headers["name"] ?: ""
                        val ip = call.request.headers["ip"] ?: ""
                        val data = call.receiveText()

                        try {
                            userAuth.registerUser(name, ip, data)
                            call.respondText("User registered successfully")
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Bad Request")
                        }
                    }
                }
            }
            registerServer?.start(wait = true)
        }
    }

    fun stopRegisterUserServer() {
        registerServer?.stop(1000, 5000)
    }
}
