package models

case class User(id: String,
                username: String,
                firstname: String,
                lastname: String,
                active: String)

object User {
  import play.api.libs.json._
  implicit val userFormats = Json.format[User]
  def writeUser(user: User) = {
    Json.toJson(user)
  }
  def readUser(jsonUser: JsValue) = {
    jsonUser.as[User]
  }
  def writeUserList(userList: List[User]) = {
    Json.toJson(userList)
  }
}