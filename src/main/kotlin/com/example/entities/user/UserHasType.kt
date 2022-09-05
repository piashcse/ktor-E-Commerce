package com.example.entities.user

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object UserHasTypeTable : BaseIntIdTable("user_has_type") {
    val userId = reference("user_id", UserTable.id)
    val userTypeId = varchar("user_type_id", 50)
}

class UserHasTypeEntity(id: EntityID<String>) : BaseIntEntity(id, UserHasTypeTable) {
    companion object : BaseIntEntityClass<UserHasTypeEntity>(UserHasTypeTable)

    var userId by UserHasTypeTable.userId
    var userTypeId by UserHasTypeTable.userTypeId
    //var users by UsersEntity referencedOn  UserHasTypeTable.user_id
    fun userHasTypeResponse() = UserHasType(id.toString(), userTypeId)
}

data class UserHasType(
    val id: String, val userTypeId: String
)