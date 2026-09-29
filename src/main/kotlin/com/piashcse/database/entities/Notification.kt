package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object NotificationTable : BaseIdTable("notification") {
    val userId = reference("user_id", UserTable.id).index()

    // EMAIL default mismatch fix: V9 created this column with DB default 'EMAIL' but
    // every writer (NotificationSubscriber, send endpoint) stores 'IN_APP'. The
    // entity default is now 'IN_APP' to match; there is deliberately no migration
    // touching the old default, so all inserts must keep setting channel explicitly
    // and must never rely on the DB-level default.
    val channel = varchar("channel", 20).default("IN_APP")
    val type = varchar("type", 50).index()
    val title = varchar("title", 255)
    val body = text("body").nullable()
    val resourceType = varchar("resource_type", 50).nullable()
    val resourceId = varchar("resource_id", 50).nullable()
    val isRead = bool("is_read").default(false)

    init {
        index(customIndexName = "notification_user_read_idx", isUnique = false, userId, isRead)
    }
}

class NotificationDAO(id: EntityID<String>) : BaseEntity(id, NotificationTable) {
    companion object : BaseEntityClass<NotificationDAO>(NotificationTable, NotificationDAO::class.java)

    var userId by NotificationTable.userId
    var channel by NotificationTable.channel
    var type by NotificationTable.type
    var title by NotificationTable.title
    var body by NotificationTable.body
    var resourceType by NotificationTable.resourceType
    var resourceId by NotificationTable.resourceId
    var isRead by NotificationTable.isRead
}
