package com.apptank.horus.client.database

import com.apptank.horus.client.base.utils.DateTime
import com.apptank.horus.client.domain.EntityAttribute
import com.apptank.horus.client.extensions.notContains

/**
 * Base class representing an action to be performed on a database table.
 *
 * @property table The name of the database table.
 */
open class ActionDatabase(open val table: String)

/**
 * Data class representing an insert action to be performed on a database table.
 *
 * @property table The name of the database table.
 * @property values A list of column-value pairs to be inserted.
 */
data class RecordInsertData(
    override val table: String,
    val values: List<DBColumnValue>
) : ActionDatabase(table)


/**
 * Data class representing an update operation on a database record.
 *
 * @property table The name of the table.
 * @property values The list of column-value pairs to be updated.
 * @property conditions The list of conditions for the update.
 * @property operator The logical operator to combine conditions (AND/OR).
 */
data class RecordUpdateData(
    override val table: String,
    val values: List<DBColumnValue>,
    val conditions: List<WhereCondition>,
    val operator: Operator = Operator.AND
) : ActionDatabase(table)


/**
 * Data class representing a delete action to be performed on a database table.
 *
 * @property table The name of the database table.
 * @property values A list of column-value pairs to be used as conditions for deletion.
 */
data class RecordDeleteData(
    override val table: String,
    val conditions: List<WhereCondition>,
    val operator: Operator = Operator.AND
) : ActionDatabase(table)

/**
 * Data class representing a column-value pair in a database table.
 *
 * @property column The name of the column.
 * @property value The value associated with the column.
 */
data class DBColumnValue(
    val column: String,
    val value: Any
)

/**
 * Data class representing a condition to be used in database operations.
 *
 * @property columnValue The column-value pair to be compared.
 * @property comparator The comparison operator to be used (e.g., '=', '!=', '<', '>').
 */
data class WhereCondition(
    val columnValue: DBColumnValue,
    val comparator: String
) {
    init {
        if (listOf(">", "<", "=", "!=").notContains(comparator)) {
            throw IllegalArgumentException("Comparator [$comparator] is not supported")
        }
    }
}

/**
 * Enum class representing logical operators to be used in combining conditions.
 */
enum class Operator {
    AND,
    OR
}

/**
 * Data class representing the result of a database operation.
 *
 * @property isSuccess Indicates if the operation was successful.
 * @property rowsAffected The number of rows affected by the operation.
 * @property isFailure Indicates if the operation was failure.
 */
data class OperationResult(
    val isSuccess: Boolean,
    val rowsAffected: Int,
    val isFailure: Boolean = !isSuccess,
)


/**
 * Entity record from database
 */
data class EntityRecord(
    val id: String,
    val userId: Int,
    val hash: String,
    val createdAt: DateTime,
    val updatedAt: DateTime,
    private val attributes: Map<String, Any>
) {
    fun getInt(attr: String): Int {
        return attributes[attr] as? Int?
            ?: throw IllegalStateException("Attribute [$attr] is not an Integer")
    }

    fun getLong(attr: String): Long {
        return attributes[attr] as? Long?
            ?: throw IllegalStateException("Attribute [$attr] is not a Long")
    }

    fun getDouble(attr: String): Double {
        return attributes[attr] as? Double?
            ?: throw IllegalStateException("Attribute [$attr] is not a Double")
    }

    fun getString(attr: String): String {
        return attributes[attr] as? String?
            ?: throw IllegalStateException("Attribute [$attr] is not a String")
    }

    fun getBoolean(attr: String): Boolean {
        return attributes[attr] as? Boolean?
            ?: throw IllegalStateException("Attribute [$attr] is not a Boolean")
    }


}


fun EntityAttribute<*>.toDBColumnValue(): DBColumnValue {
    return DBColumnValue(name, value!!)
}

fun List<EntityAttribute<*>>.mapToDBColumValue(): List<DBColumnValue> {
    return this.map { it.toDBColumnValue() }
}