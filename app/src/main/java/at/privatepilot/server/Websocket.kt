package at.privatepilot.server

import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.concurrent.CopyOnWriteArrayList

class Websocket(port: Int) {
   /* private val connections = CopyOnWriteArrayList<DefaultWebSocketSession>()

    init {
        embeddedServer(Netty, port) {
            install(ContentNegotiation) {
                jackson {}
            }

            routing {
                webSocket("/ws") {
                    try {
                        send("You are connected!")

                        for (frame in incoming) {
                            frame as? Frame.Text ?: continue
                            val receivedText = frame.readText()

                            // Handle WebSocket message

                            // Example: Broadcast the received message to all connected clients
                            connections.forEach { it.send(receivedText) }
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        // Connection closed
                    } finally {
                        // Remove closed connection
                        connections.remove(this)
                    }
                }
            }
        }.start(wait = true)
    }*/
}