package controllers

import javax.inject._

import play.api.db.Database
import play.api.mvc._
import models._
import models.User._
import models.Group._
import play.api.libs.json.JsValue
import play.api.libs.json._

class SCIMController @Inject() (db:Database) extends Controller {

  def users(filter:Option[String], count:Option[String], startIndex:Option[String]) = Action {
    // TODO: Retrieve paginated User Objects
    // TODO: Allow for an equals and startsWith filters on username
    val conn = db.getConnection()
    var users = List[User]()
    try{
      val query = conn.createStatement()
      var statement = "SELECT * FROM users"
      if(!filter.isEmpty){
        statement += " WHERE username LIKE '%" + filter.getOrElse("") + "%'"
      }

      if(!startIndex.isEmpty && !count.isEmpty){
        statement += " LIMIT " + startIndex.getOrElse("") + "," + count.getOrElse("")
      }else if(startIndex.isEmpty && !count.isEmpty){
        statement += " LIMIT " + count.getOrElse("")
      }else if(!startIndex.isEmpty && count.isEmpty){
        statement += " OFFSET " + startIndex.getOrElse("")
      }

      val rs = query.executeQuery(statement)


      while (rs.next()) {
        users = User(rs.getString("id"), rs.getString("username"), rs.getString("firstname"), rs.getString("lastname"), rs.getString("active")) :: users
      }

    } finally {
      conn.close()
    }
    val userListJson = writeUserList(users)
    Ok(userListJson)

  }

  def user(uid:String)  = Action {
    // TODO: Retrieve a single User Object by ID

    val conn = db.getConnection()
    var userdata: User = null
    var check = true
    try{
      val query = conn.createStatement()
      val rs = query.executeQuery("SELECT * FROM users WHERE id = " + uid)

      while (rs.next()) {
        userdata = User(rs.getString("id"), rs.getString("username"), rs.getString("firstname"), rs.getString("lastname"), rs.getString("active"))
      }
    } finally {
      conn.close()
    }
    if(userdata != null) {
      val userJSON = writeUser(userdata)
      Ok(userJSON)
    }else{
      Ok("{\"message\":\"Invalid User ID.\"}")
    }
  }

  def createUser() = Action(parse.tolerantFormUrlEncoded) { implicit request =>
    // TODO: Create a User Object with firstname and lastname metadata
    val conn = db.getConnection()
    val username = request.body.get("username").map(_.head).getOrElse("")
    val password = request.body.get("password").map(_.head).getOrElse("")
    val firstname = request.body.get("firstname").map(_.head).getOrElse("")
    val lastname = request.body.get("lastname").map(_.head).getOrElse("")

    try {
      val query = conn.createStatement()
      query.execute("INSERT INTO `users` (`username`,`password`,`firstname`, `lastname`, `active`) VALUES ('" + username + "', '" + password + "', '" + firstname + "', '" + lastname + "', '1')")
    } finally {
      conn.close()
    }
    Ok("{\"message\":\"User has been created.\"}")
  }

  def updateUser(uid:String) = Action(parse.tolerantFormUrlEncoded) { implicit request =>
    // TODO: Update a User Object's firstname, lastname, and active status
    val conn = db.getConnection()
    val firstname = request.body.get("firstname").map(_.head).getOrElse("")
    val lastname = request.body.get("lastname").map(_.head).getOrElse("")

    try {
      val query = conn.createStatement()
      query.execute("UPDATE `users` SET `firstname` = '" + firstname + "', `lastname` = '" + lastname + "' WHERE `users`.`id` = " + uid)
    } finally {
      conn.close()
    }
    Ok("{\"message\":\"User has been updated.\"}")
  }

  def deleteUser(uid:String) = Action {
    // TODO: Delete a User Object by ID
    val conn = db.getConnection()

    try {
      val query = conn.createStatement()
      query.execute("DELETE FROM users WHERE id = " + uid)
    } finally {
      conn.close()
    }
    Ok("{\"message\":\"UserId " + uid + " has been deleted successfully.\"")
  }

  def groups(count:Option[String], startIndex:Option[String]) = Action {
    // TODO: Retrieve paginated Group Objects

    var outString = ""
    val conn = db.getConnection()
    var groups = List[Group]()
    try{

      val query = conn.createStatement()
      var statement = "SELECT * FROM groups"


      if(!startIndex.isEmpty && !count.isEmpty){
        statement += " LIMIT " + startIndex.getOrElse("") + "," + count.getOrElse("")
      }else if(startIndex.isEmpty && !count.isEmpty){
        statement += " LIMIT " + count.getOrElse("")
      }else if(!startIndex.isEmpty && count.isEmpty){
        statement += " OFFSET " + startIndex.getOrElse("")
      }

      val rs = query.executeQuery(statement)

      while (rs.next()) {
        var members = ""
        val query_members = conn.createStatement()
        val rs_members = query_members.executeQuery("SELECT b.id, b.username, b.firstname, b.lastname, b.active FROM group_members a LEFT JOIN users b ON a.user_id = b.id  WHERE a.group_id = " + rs.getString("id"))
        var users = List[User]()
        while (rs_members.next()) {
          users = User(rs_members.getString("id"), rs_members.getString("username"), rs_members.getString("firstname"), rs_members.getString("lastname"), rs_members.getString("active")) :: users
        }
        groups = Group(rs.getString("id"), rs.getString("name"), users) :: groups
      }
    } finally {
      conn.close()
    }
    val groupsJson = writeGroupList(groups)
//    Ok("[" + outString.dropRight(1) + "]")
    Ok(groupsJson)
  }

  def group(groupId:String) = Action {
    // TODO: Retrieve a single Group Object by ID
    var members = ""
    var outString = ""
    val conn = db.getConnection()
    var group: Group = null
    var check = true
    try{
      val query_members = conn.createStatement()
      val rs_members = query_members.executeQuery("SELECT b.id, b.username, b.firstname, b.lastname, b.active FROM group_members a LEFT JOIN users b ON a.user_id = b.id  WHERE a.group_id = " + groupId)
      var userList = List[User]()
      while (rs_members.next()) {
        userList = User(rs_members.getString("id"), rs_members.getString("username"), rs_members.getString("firstname"), rs_members.getString("lastname"), rs_members.getString("active")) :: userList
      }

      val query = conn.createStatement()
      val rs = query.executeQuery("SELECT * FROM groups WHERE id = " + groupId)

      while (rs.next()) {
          group = Group(rs.getString("id"), rs.getString("name"), userList)
      }
    } finally {
      conn.close()
    }
    if(group != null){
      val groupJSON = writeGroup(group)
      Ok(groupJSON)
    }else{
      Ok("{\"message\":\"Invalid Group ID\"}")
    }

  }

  def patchGroup(groupId:String) = Action { implicit request =>
    // TODO: Patch a Group Object, modifying its members
    val conn = db.getConnection()
    val jsonData = request.body.asJson.getOrElse("").toString
    val groupList: JsValue = Json.parse(jsonData)
    val group = readGroup(groupList)
    val users = group.members
    try {
      for (user <- users) {
        val query = conn.createStatement()
        query.execute("UPDATE `users` SET `username` = '" + user.username + "', `firstname` = '" + user.firstname + "', `lastname` = '" + user.lastname + "', `active` = '" + user.active + "' WHERE `users`.`id` = " + user.id)
      }
    }finally {
      conn.close()
    }

    Ok("{\"message\":\"Group \"" + group.name + "\" members has been updated.\"}")
  }
}


