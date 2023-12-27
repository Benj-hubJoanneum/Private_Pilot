package at.privatepilot.server.model.users

data class User(
    val name: String,
    val ip: String,
    val token: Token
)
