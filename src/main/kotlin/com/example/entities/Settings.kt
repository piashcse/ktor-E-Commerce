package com.example.entities

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object SettingTable : BaseIntIdTable("settings") {
    val shippingCharge = integer("shipping_charge")
    val shopName = text("shop_name")
    val email = varchar("email", 50)
    val phone = integer("phone")
    val address = text("address")
    val logo = text("logo")
}

class SettingEntity(id: EntityID<String>) : BaseIntEntity(id, SettingTable) {
    companion object : BaseIntEntityClass<SettingEntity>(SettingTable)

    var shippingCharge by SettingTable.shippingCharge
    var shopName by SettingTable.shopName
    var email by SettingTable.email
    var phone by SettingTable.phone
    var address by SettingTable.address
    var logo by SettingTable.logo
}
