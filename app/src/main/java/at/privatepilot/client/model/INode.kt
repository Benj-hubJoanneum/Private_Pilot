package at.privatepilot.client.model

interface INode {
    var parentFolder : String
    var name : String
    var path : String
    var type : FileType
}