package at.privatepilot.client.restapi.model

interface IMetadata {
    val name: String
    val path: String
    val size: Int
    val last_modified: Double
}