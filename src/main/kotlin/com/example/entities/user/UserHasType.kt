package com.example.entities.user

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object UserHasTypeTable : BaseIntIdTable("user_has_type") {
    val user_id = reference("user_id", UserTable.id)
    val user_type_id = varchar("user_type_id", 50)
}

class UserHasTypeEntity(id: EntityID<String>) : BaseIntEntity(id, UserHasTypeTable) {
    companion object : BaseIntEntityClass<UserHasTypeEntity>(UserHasTypeTable)

    var user_id by UserHasTypeTable.user_id
    var user_type_id by UserHasTypeTable.user_type_id
    //var users by UsersEntity referencedOn  UserHasTypeTable.user_id
    fun userHasTypeResponse() = UserHasType(id.toString(), user_type_id)
}

data class UserHasType(
    val id: String, val userTypeId: String
)