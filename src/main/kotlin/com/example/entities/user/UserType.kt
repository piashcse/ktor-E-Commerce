package com.example.entities.user

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object UserTypeTable : BaseIntIdTable("user_type") {
    val userType = text("user_type")
}

class UserTypeEntity(id: EntityID<String>) : BaseIntEntity(id, UserTypeTable)  {
    companion object : BaseIntEntityClass< UserTypeEntity>(UserTypeTable)
    var userType by UserTypeTable.userType
}
