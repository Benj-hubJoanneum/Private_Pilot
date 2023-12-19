package at.privatepilot.server

data class Token(
    val hashed: String,
    val salt: String
)