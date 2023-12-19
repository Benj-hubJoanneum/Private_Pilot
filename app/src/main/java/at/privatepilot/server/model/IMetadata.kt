package at.privatepilot.server.model

interface IMetadata {
    val name: String
    val path: String
    val size: Int
    val last_modified: Double
}