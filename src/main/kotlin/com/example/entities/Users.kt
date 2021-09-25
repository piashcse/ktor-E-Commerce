package com.example.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object UsersTable : IdTable<String>("users") {
    override val id: Column<EntityID<String>> = text("user_id").uniqueIndex().entityId()
    val user_name = text("user_name")
    val email = text("email")
    val password = text("password")
    val mobile_number = text("mobile_number").nullable()
    val email_verified_at = text("email_verified_at").nullable()
    val remember_token = text("remember_token").nullable()
    val created_at = text("created_at").nullable()
    val updated_at = text("updated_at").nullable()
    val is_verified = text("is_verified").nullable()
    override val primaryKey = PrimaryKey(id)
}

class UsersEntity(id: EntityID<String>) : Entity<String>(id)  {
    companion object : EntityClass<String, UsersEntity>(UsersTable)
    var userId by UsersTable.id
    var user_name by UsersTable.user_name
    var email by UsersTable.email
    var password by UsersTable.password
    var mobile_number by UsersTable.mobile_number
    var email_verified_at by UsersTable.email_verified_at
    var remember_token by UsersTable.remember_token
    var created_at by UsersTable.created_at
    var updated_at by UsersTable.updated_at
    var is_verified by UsersTable.is_verified
    fun userResponse() = UsersResponse(userId.value, user_name,email, mobile_number, email_verified_at, remember_token, created_at, updated_at, is_verified)
}
data class UsersResponse(  val userId :String,
                           val userName :String,
                           val email :String,
                           val mobileNumber:String?,
                           val emailVerifiedAt :String?,
                           val rememberToken:String?,
                           val createdAt:String?,
                           val updatedAt :String?,
                           val isVerified :String?)

data class ChangePassword(val oldPassword:String, val newPassword:String)
