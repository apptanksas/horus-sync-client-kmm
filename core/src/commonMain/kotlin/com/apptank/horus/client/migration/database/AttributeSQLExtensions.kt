package com.apptank.horus.client.migration.database

import com.apptank.horus.client.migration.domain.Attribute
import com.apptank.horus.client.migration.domain.AttributeType

fun Attribute.convertToSQL(): String {
    var sql = this.name + " " + when (this.type) {
        AttributeType.PrimaryKeyInteger -> "INTEGER PRIMARY KEY"
        AttributeType.PrimaryKeyString -> "TEXT PRIMARY KEY"
        AttributeType.PrimaryKeyUUID -> "TEXT PRIMARY KEY"
        AttributeType.Integer -> "INTEGER"
        AttributeType.String -> "TEXT"
        AttributeType.Float -> "FLOAT"
        AttributeType.Boolean -> "BOOLEAN"
        AttributeType.Text -> "TEXT"
        AttributeType.Json -> "TEXT"
        AttributeType.Enum -> "TEXT"
        AttributeType.Timestamp -> "INTEGER"
        AttributeType.UUID -> "TEXT"
        else -> throw IllegalArgumentException("Attribute type ${this.type} not defined")
    }

    if (!isNullable) {
        sql += " NOT NULL"
    }

    return sql
}