package models

case class Group(id: String,
                name: String,
                members: List[User])

object Group {
  import play.api.libs.json._
  implicit val groupFormats = Json.format[Group]
  def writeGroup(group: Group) = {
    Json.toJson(group)
  }
  def readGroup(jsonGroup: JsValue) = {
    jsonGroup.as[Group]
  }
  def writeGroupList(groupList: List[Group]) = {
    Json.toJson(groupList)
  }
  def readGroupList(jsonGroupList: JsValue) = {
    jsonGroupList.as[List[Group]]
  }
}
