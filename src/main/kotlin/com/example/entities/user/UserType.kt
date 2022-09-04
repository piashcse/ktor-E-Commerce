package com.example.entities.user

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object UserTypeTable : BaseIntIdTable("user_type") {
    val user_type = text("user_type")
    override val primaryKey = PrimaryKey(id)
}

class UserTypeEntity(id: EntityID<String>) : BaseIntEntity(id, UserTypeTable)  {
    companion object : BaseIntEntityClass< UserTypeEntity>(UserTypeTable)
    var user_type by UserTypeTable.user_type
}
