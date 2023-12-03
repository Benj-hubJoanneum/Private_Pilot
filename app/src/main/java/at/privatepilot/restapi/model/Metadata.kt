package at.privatepilot.restapi.model

class Metadata(
    override val name: String,
    override val path: String,
    val type: String,
    override val size: Int,
    override val last_modified: Double
) : IMetadata {
}