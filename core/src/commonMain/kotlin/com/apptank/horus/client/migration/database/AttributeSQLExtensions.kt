package com.apptank.horus.client.migration.database

import com.apptank.horus.client.migration.domain.Attribute
import com.apptank.horus.client.migration.domain.AttributeType
import com.apptank.horus.client.migration.domain.Constraint
import com.apptank.horus.client.migration.domain.ConstraintType

fun Attribute.convertToSQL(applyConstraints: (List<Constraint>) -> Unit = {}): String {

    val constraints = mutableListOf<Constraint>()

    var sql = this.name + " " + when (this.type) {
        AttributeType.PrimaryKeyInteger -> "INTEGER PRIMARY KEY"
        AttributeType.PrimaryKeyString -> "TEXT PRIMARY KEY"
        AttributeType.PrimaryKeyUUID -> "TEXT PRIMARY KEY"
        AttributeType.Integer -> "INTEGER"
        AttributeType.String -> "TEXT"
        AttributeType.Float -> "REAL"
        AttributeType.Boolean -> "BOOLEAN"
        AttributeType.Text -> "TEXT"
        AttributeType.Json -> "TEXT"
        AttributeType.Enum -> "TEXT CHECK ($name IN (${this.options.joinToString(", ") { "'$it'" }}))"
        AttributeType.Timestamp -> "TEXT"
        AttributeType.UUID -> "TEXT"
        else -> throw IllegalArgumentException("Attribute type ${this.type} not defined")
    }

    if (!isNullable) {
        sql += " NOT NULL"
    }

    // Apply constraints
    if (linkedEntity?.isNotBlank() == true) {
        constraints.add(
            Constraint(
                ConstraintType.FOREIGN_KEY,
                "FOREIGN KEY ($name) REFERENCES $linkedEntity(id) ON DELETE CASCADE"
            )
        )
    }

    applyConstraints(constraints)

    return sql
}