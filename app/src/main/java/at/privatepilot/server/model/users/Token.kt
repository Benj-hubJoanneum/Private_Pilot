package at.privatepilot.server.model.users

data class Token(
    val hashed: String,
    val salt: String
)