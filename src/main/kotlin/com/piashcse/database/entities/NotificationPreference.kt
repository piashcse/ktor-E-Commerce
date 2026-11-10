package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** Per-event notification channel preference. */
object NotificationPreferenceTable : BaseIdTable("notification_preference") {
    val userId = reference("user_id", UserTable.id).index()
    val eventType = varchar("event_type", 50)
    val channel = varchar("channel", 20)

    init {
        uniqueIndex("notification_preference_user_event_unique", userId, eventType)
    }
}

class NotificationPreferenceDAO(id: EntityID<String>) : BaseEntity(id, NotificationPreferenceTable) {
    companion object : BaseEntityClass<NotificationPreferenceDAO>(NotificationPreferenceTable, NotificationPreferenceDAO::class.java)

    var userId by NotificationPreferenceTable.userId
    var eventType by NotificationPreferenceTable.eventType
    var channel by NotificationPreferenceTable.channel
}
