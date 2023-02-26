package com.example.entities.coupon

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object CouponsTable : BaseIntIdTable("coupons") {
    val coupon = varchar("coupon", 50)
    val discount = integer("discount")
}

class CouponsEntity(id: EntityID<String>) : BaseIntEntity(id, CouponsTable) {
    companion object : BaseIntEntityClass<CouponsEntity>(CouponsTable)

    var coupons by CouponsTable.coupon
    var discount by CouponsTable.discount
}