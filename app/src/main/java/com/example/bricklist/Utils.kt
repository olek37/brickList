package com.example.bricklist

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.util.*

@JsonRootName("ITEM")
data class Item(

    @set:JsonProperty("ITEMTYPE")
    var type: String? = null,

    @set:JsonProperty("ITEMID")
    var id: String? = null,

    @set:JsonProperty("QTY")
    var quantity: Int? = null,

    @set:JsonProperty("COLOR")
    var color: String? = null,

    @set:JsonProperty("EXTRA")
    var extra: String? = null,

    @set:JsonProperty("ALTERNATE")
    var alternate: String? = null,

    @set:JsonProperty("MATCHID")
    var match: String? = null,

    @set:JsonProperty("COUNTERPART")
    var counterpart: String? = null
)

@JsonRootName("INVENTORY")
data class Inventory(

    @set:JsonProperty("ITEM")
    var items: List<Item> = ArrayList()
)

internal val kotlinXmlMapper = XmlMapper(JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}).registerKotlinModule()
    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


internal inline fun <reified T : Any> parseAs(resource : File): T {
    return kotlinXmlMapper.readValue(resource)
}
