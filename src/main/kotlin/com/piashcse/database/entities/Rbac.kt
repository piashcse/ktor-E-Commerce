package com.piashcse.database.entities

import com.piashcse.constants.InvitationStatus
import com.piashcse.constants.MembershipStatus
import com.piashcse.constants.RoleScope
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

/** Platform and shop roles share one catalog; [scopeType] selects the portal. */
object AuthRoleTable : BaseIdTable("auth_role") {
    val code = varchar("code", 100).uniqueIndex()
    val name = varchar("name", 150)
    val description = text("description").nullable()
    val scopeType = varchar("scope_type", 20).default(RoleScope.GLOBAL)
    val isSystem = bool("is_system").default(false)
}

class AuthRoleDAO(id: EntityID<String>) : BaseEntity(id, AuthRoleTable) {
    companion object : BaseEntityClass<AuthRoleDAO>(AuthRoleTable, AuthRoleDAO::class.java)

    var code by AuthRoleTable.code
    var name by AuthRoleTable.name
    var description by AuthRoleTable.description
    var scopeType by AuthRoleTable.scopeType
    var isSystem by AuthRoleTable.isSystem
}

object AuthPermissionTable : BaseIdTable("auth_permission") {
    val code = varchar("code", 120).uniqueIndex()
    val resource = varchar("resource", 60)
    val action = varchar("action", 60)
    val description = text("description").nullable()
    val isSystem = bool("is_system").default(true)
}

class AuthPermissionDAO(id: EntityID<String>) : BaseEntity(id, AuthPermissionTable) {
    companion object : BaseEntityClass<AuthPermissionDAO>(AuthPermissionTable, AuthPermissionDAO::class.java)

    var code by AuthPermissionTable.code
    var resource by AuthPermissionTable.resource
    var action by AuthPermissionTable.action
    var description by AuthPermissionTable.description
    var isSystem by AuthPermissionTable.isSystem
}

object AuthRolePermissionTable : Table("auth_role_permission") {
    val roleId = reference("role_id", AuthRoleTable.id)
    val permissionId = reference("permission_id", AuthPermissionTable.id)

    override val primaryKey = PrimaryKey(roleId, permissionId)
}

/** Platform role assignment. Shop assignments live in [ShopMemberRoleTable]. */
object AuthUserRoleTable : Table("auth_user_role") {
    val userId = reference("user_id", UserTable.id)
    val roleId = reference("role_id", AuthRoleTable.id)
    val assignedBy = reference("assigned_by", UserTable.id).nullable()
    val assignedAt = datetime("assigned_at").clientDefault { currentUtc() }

    override val primaryKey = PrimaryKey(userId, roleId)
}

object ShopMemberTable : BaseIdTable("shop_member") {
    val shopId = reference("shop_id", ShopTable.id)
    val userId = reference("user_id", UserTable.id)
    val status = varchar("status", 20).default(MembershipStatus.ACTIVE)
    val isOwner = bool("is_owner").default(false)
    val invitedBy = reference("invited_by", UserTable.id).nullable()
    val joinedAt = datetime("joined_at").nullable()

    init {
        uniqueIndex("shop_member_shop_user_unique", shopId, userId)
    }
}

class ShopMemberDAO(id: EntityID<String>) : BaseEntity(id, ShopMemberTable) {
    companion object : BaseEntityClass<ShopMemberDAO>(ShopMemberTable, ShopMemberDAO::class.java)

    var shopId by ShopMemberTable.shopId
    var userId by ShopMemberTable.userId
    var status by ShopMemberTable.status
    var isOwner by ShopMemberTable.isOwner
    var invitedBy by ShopMemberTable.invitedBy
    var joinedAt by ShopMemberTable.joinedAt
}

object ShopMemberRoleTable : Table("shop_member_role") {
    val memberId = reference("member_id", ShopMemberTable.id)
    val roleId = reference("role_id", AuthRoleTable.id)

    override val primaryKey = PrimaryKey(memberId, roleId)
}

object ShopInvitationTable : BaseIdTable("shop_invitation") {
    val shopId = reference("shop_id", ShopTable.id)
    val email = varchar("email", 255)
    val roleIds = text("role_ids")
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val status = varchar("status", 20).default(InvitationStatus.PENDING)
    val invitedBy = reference("invited_by", UserTable.id).nullable()
    val expiresAt = datetime("expires_at")
    val acceptedAt = datetime("accepted_at").nullable()
}

class ShopInvitationDAO(id: EntityID<String>) : BaseEntity(id, ShopInvitationTable) {
    companion object : BaseEntityClass<ShopInvitationDAO>(ShopInvitationTable, ShopInvitationDAO::class.java)

    var shopId by ShopInvitationTable.shopId
    var email by ShopInvitationTable.email
    var roleIds by ShopInvitationTable.roleIds
    var tokenHash by ShopInvitationTable.tokenHash
    var status by ShopInvitationTable.status
    var invitedBy by ShopInvitationTable.invitedBy
    var expiresAt by ShopInvitationTable.expiresAt
    var acceptedAt by ShopInvitationTable.acceptedAt
}

object MemberWarehouseTable : Table("member_warehouse") {
    val memberId = reference("member_id", ShopMemberTable.id)
    val warehouseId = reference("warehouse_id", WarehouseTable.id)

    override val primaryKey = PrimaryKey(memberId, warehouseId)
}
