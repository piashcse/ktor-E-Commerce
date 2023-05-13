package com.piashcse.entities.user

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object UserTypeTable : BaseIntIdTable("user_type") {
    val userType = text("user_type")
}

class UserTypeEntity(id: EntityID<String>) : BaseIntEntity(id, UserTypeTable)  {
    companion object : BaseIntEntityClass< UserTypeEntity>(UserTypeTable)
    var userType by UserTypeTable.userType
}
