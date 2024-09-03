package com.apptank.horus.client.data


sealed class Horus {

    data class Entity(
        val name: String,
        val attributes: List<Attribute<*>>,
        val relations: Map<String, List<Entity>>? = null
    ) {
        fun getInt(name: String): Int = attributes.find { it.name == name }?.value as Int

        fun getString(name: String): String = attributes.find { it.name == name }?.value as String

        fun getBoolean(name: String): Boolean =
            attributes.find { it.name == name }?.value as Boolean

        fun getDouble(name: String): Double = attributes.find { it.name == name }?.value as Double

        fun getFloat(name: String): Float = attributes.find { it.name == name }?.value as Float

        fun getLong(name: String): Long = attributes.find { it.name == name }?.value as Long
    }

    data class Attribute<T>(
        val name: String,
        val value: T
    )
}

