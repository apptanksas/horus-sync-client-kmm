package com.apptank.horus.client.data

data class EntityData(
    val name: String,
    val attributes: List<EntityAttribute<*>>
) {
    fun getInt(name: String): Int = attributes.find { it.name == name }?.value as Int

    fun getString(name: String): String = attributes.find { it.name == name }?.value as String

    fun getBoolean(name: String): Boolean = attributes.find { it.name == name }?.value as Boolean

    fun getDouble(name: String): Double = attributes.find { it.name == name }?.value as Double

    fun getFloat(name: String): Float = attributes.find { it.name == name }?.value as Float

    fun getLong(name: String): Long = attributes.find { it.name == name }?.value as Long
}