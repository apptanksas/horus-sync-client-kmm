package org.apptank.horus.client.migration.database

import org.apptank.horus.client.migration.domain.Attribute
import org.apptank.horus.client.migration.domain.AttributeNameMasker
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.migration.domain.Constraint
import org.apptank.horus.client.migration.domain.ConstraintType

/**
 * This class represents an attribute that can be converted into a SQL column definition.
 *
 * @year 2024
 * @author John Ospina
 */

fun Attribute.convertToSQL(applyConstraints: (List<Constraint>) -> Unit = {}): String {

    // Create a mutable list to hold any constraints
    val constraints = mutableListOf<Constraint>()
    val masker = this.name
    val attributeName = this.name

    // Start building the SQL column definition
    var sql = "$attributeName " + when (this.type) {
        AttributeType.PrimaryKeyInteger -> "INTEGER PRIMARY KEY"
        AttributeType.PrimaryKeyString -> "TEXT PRIMARY KEY"
        AttributeType.PrimaryKeyUUID -> "TEXT PRIMARY KEY"
        AttributeType.Integer -> "INTEGER"
        AttributeType.String -> "TEXT"
        AttributeType.Float -> "REAL"
        AttributeType.Boolean -> "BOOLEAN"
        AttributeType.Text -> "TEXT"
        AttributeType.Json -> "TEXT"
        AttributeType.Enum -> "TEXT CHECK ($attributeName IN (${this.options.joinToString(", ") { "'$it'" }}))"
        AttributeType.Timestamp -> "TEXT"
        AttributeType.UUID -> "TEXT"
        AttributeType.RefFile -> "TEXT"
        else -> throw IllegalArgumentException("Attribute type ${this.type} not defined")
    }

    // Add NOT NULL constraint if the attribute is not nullable
    if (!isNullable) {
        sql += " NOT NULL"
    }

    // Apply constraints such as foreign key relationships
    if (linkedEntity?.isNotBlank() == true) {
        constraints.add(
            Constraint(
                ConstraintType.FOREIGN_KEY,
                "FOREIGN KEY ($attributeName) REFERENCES $linkedEntity(id) ON DELETE CASCADE"
            )
        )
    }

    // Execute the applyConstraints function with the list of constraints
    applyConstraints(constraints)

    // Return the final SQL column definition
    return sql
}
