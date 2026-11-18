package com.piashcse.feature.rbac

import com.piashcse.constants.InvitationStatus
import com.piashcse.constants.MembershipStatus
import com.piashcse.constants.RbacDefaults
import com.piashcse.constants.RoleScope
import com.piashcse.database.entities.AuthRoleTable
import com.piashcse.database.entities.MemberWarehouseTable
import com.piashcse.database.entities.ShopInvitationDAO
import com.piashcse.database.entities.ShopInvitationTable
import com.piashcse.database.entities.ShopMemberDAO
import com.piashcse.database.entities.ShopMemberRoleTable
import com.piashcse.database.entities.ShopMemberTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.utils.common.sha256Hex
import com.piashcse.utils.common.toHex
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.entityIDs
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.security.SecureRandom

/** Shop staff membership and invitations. Platform RBAC lives in [RbacService]. */
class ShopMembershipService {
    private val random = SecureRandom()
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    // ── Shop memberships ─────────────────────────────────────────────────

    suspend fun listShopMembers(actorUserId: String): List<ShopMemberResponse> {
        val shopId = ownerShopId(actorUserId)
        return query {
            val rows =
                ShopMemberTable.selectAll()
                    .where {
                        (ShopMemberTable.shopId eq shopId.entityID(ShopTable)) and
                            (ShopMemberTable.status neq MembershipStatus.REMOVED)
                    }
                    .orderBy(ShopMemberTable.isOwner to SortOrder.DESC, ShopMemberTable.createdAt to SortOrder.ASC)
                    .toList()
            val memberIds = rows.map { it[ShopMemberTable.id].value }
            val roleCodes = roleCodesByMemberIds(memberIds)
            val userIds = rows.map { it[ShopMemberTable.userId].value }.distinct()
            val emails =
                if (userIds.isEmpty()) {
                    emptyMap()
                } else {
                    UserDAO.find { UserTable.id inList userIds.entityIDs(UserTable) }
                        .associate { it.id.value to it.email }
                }
            rows.map { row ->
                val member = ShopMemberDAO.wrapRow(row)
                member.toMember(emails[member.userId.value].orEmpty())
                    .copy(roles = roleCodes[member.id.value].orEmpty())
            }
        }
    }

    suspend fun inviteShopMember(
        actorUserId: String,
        request: InviteShopMemberRequest,
    ): InvitationCreatedResponse {
        val shopId = ownerShopId(actorUserId)
        val email = request.email.trim().lowercase()
        if (!email.matches(emailPattern)) throw ValidationException("Valid email is required")
        val roles = query { shopRoles(request.roleIds) }

        val raw = ByteArray(32).also { random.nextBytes(it) }.toHex()
        val invitation =
            query {
                ShopInvitationDAO.new {
                    this.shopId = shopId.entityID(ShopTable)
                    this.email = email
                    roleIds = roles.joinToString(",") { it.id }
                    tokenHash = sha256Hex(raw)
                    status = InvitationStatus.PENDING
                    invitedBy = actorUserId.entityID(UserTable)
                    expiresAt = currentUtc().plusDays(7)
                }
            }
        AdminAudit.publish(actorUserId, "SHOP_MEMBER_INVITED", "SHOP", shopId, email)
        return InvitationCreatedResponse(
            id = invitation.id.value,
            shopId = shopId,
            email = email,
            expiresAt = invitation.expiresAt.toString(),
            token = raw,
        )
    }

    suspend fun acceptInvitation(
        userId: String,
        token: String,
    ): ShopMemberResponse {
        val hash = sha256Hex(token)
        val (invitation, member) =
            query {
                val invitation =
                    ShopInvitationDAO.find { ShopInvitationTable.tokenHash eq hash }.firstOrNull()
                        ?: throw ValidationException("Invitation not found")
                if (invitation.status != InvitationStatus.PENDING) throw ValidationException("Invitation is no longer active")
                if (invitation.expiresAt.isBefore(currentUtc())) {
                    invitation.status = InvitationStatus.EXPIRED
                    throw ValidationException("Invitation has expired")
                }
                val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
                if (!user.email.equals(invitation.email, ignoreCase = true)) {
                    throw ForbiddenException("Invitation belongs to another email address")
                }
                val member =
                    ShopMemberDAO.find {
                        (ShopMemberTable.shopId eq invitation.shopId) and (ShopMemberTable.userId eq user.id)
                    }.firstOrNull() ?: ShopMemberDAO.new {
                        shopId = invitation.shopId
                        this.userId = user.id
                        status = MembershipStatus.ACTIVE
                        isOwner = false
                        invitedBy = invitation.invitedBy
                        joinedAt = currentUtc()
                    }
                member.status = MembershipStatus.ACTIVE
                member.joinedAt = currentUtc()
                invitation.status = InvitationStatus.ACCEPTED
                invitation.acceptedAt = currentUtc()
                val invitedRoleIds = invitation.roleIds.split(',').filter { it.isNotBlank() }.distinct()
                val invitedRoles = rolesByIds(invitedRoleIds).values
                if (invitedRoles.size != invitedRoleIds.size) {
                    throw ValidationException("One or more invited roles no longer exist")
                }
                if (invitedRoles.any { it.scopeType != RoleScope.SHOP }) {
                    throw ValidationException("Invitations may only contain SHOP roles")
                }
                ShopMemberRoleTable.deleteWhere { ShopMemberRoleTable.memberId eq member.id }
                invitedRoleIds.forEach { roleId ->
                    ShopMemberRoleTable.insert {
                        it[ShopMemberRoleTable.memberId] = member.id
                        it[ShopMemberRoleTable.roleId] = roleId.entityID(AuthRoleTable)
                    }
                }
                user.authzVersion += 1
                invitation to member
            }
        return query { member.toMember().copy(roles = memberRoleCodes(member.id.value)) }
    }

    suspend fun updateShopMemberRoles(
        actorUserId: String,
        memberId: String,
        roleIds: List<String>,
    ): ShopMemberResponse {
        val shopId = ownerShopId(actorUserId)
        val roles = query { shopRoles(roleIds) }
        val member =
            query {
                val member = ShopMemberDAO.findById(memberId) ?: memberId.throwNotFound("Shop member")
                if (member.shopId.value != shopId) throw ForbiddenException("Member belongs to another shop")
                if (member.isOwner) throw ValidationException("The shop owner role cannot be changed")
                ShopMemberRoleTable.deleteWhere { ShopMemberRoleTable.memberId eq member.id }
                roles.forEach { role ->
                    ShopMemberRoleTable.insert {
                        it[ShopMemberRoleTable.memberId] = member.id
                        it[ShopMemberRoleTable.roleId] = role.id.entityID(AuthRoleTable)
                    }
                }
                val user = UserDAO.findById(member.userId.value)
                user?.let { it.authzVersion += 1 }
                member
            }
        AdminAudit.publish(actorUserId, "SHOP_MEMBER_ROLES_UPDATED", "SHOP_MEMBER", memberId, roleIds.joinToString())
        return query { member.toMember().copy(roles = memberRoleCodes(member.id.value)) }
    }

    suspend fun removeShopMember(
        actorUserId: String,
        memberId: String,
    ) {
        val shopId = ownerShopId(actorUserId)
        query {
            val member = ShopMemberDAO.findById(memberId) ?: memberId.throwNotFound("Shop member")
            if (member.shopId.value != shopId) throw ForbiddenException("Member belongs to another shop")
            if (member.isOwner) throw ValidationException("The shop owner cannot be removed")
            member.status = MembershipStatus.REMOVED
            ShopMemberRoleTable.deleteWhere { ShopMemberRoleTable.memberId eq member.id }
            MemberWarehouseTable.deleteWhere { MemberWarehouseTable.memberId eq member.id }
            UserDAO.findById(member.userId.value)?.let { it.authzVersion += 1 }
        }
        AdminAudit.publish(actorUserId, "SHOP_MEMBER_REMOVED", "SHOP_MEMBER", memberId)
    }

    private suspend fun ownerShopId(actorUserId: String): String =
        query {
            requireSellerByUserId(actorUserId).shopId?.value
                ?: throw ValidationException("Seller has no shop yet")
        }

    private fun shopRoles(roleIds: List<String>): List<RoleRef> {
        if (roleIds.isEmpty()) throw ValidationException("At least one role is required")
        val roles = rolesByIds(roleIds).values.map { it.toRoleRef() }
        if (roles.size != roleIds.distinct().size) throw ValidationException("One or more roles were not found")
        if (roles.any { it.scopeType != RoleScope.SHOP }) throw ValidationException("Only SHOP roles can be assigned to shop members")
        if (roles.any { it.code == RbacDefaults.SHOP_OWNER_ROLE_CODE }) {
            throw ValidationException("Shop ownership must be transferred explicitly, not assigned as a role")
        }
        return roles
    }

    private fun memberRoleCodes(memberId: String): List<String> = roleCodesByMemberIds(listOf(memberId))[memberId].orEmpty()
}
