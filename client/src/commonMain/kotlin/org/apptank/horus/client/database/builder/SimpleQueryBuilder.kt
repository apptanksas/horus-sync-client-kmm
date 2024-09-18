package org.apptank.horus.client.database.builder

class SimpleQueryBuilder(
    private val tableName: String
) : QueryBuilder() {

   init {
        if (tableName.isEmpty()) {
            throw IllegalArgumentException("tableName cannot be empty")
        }
    }

   override fun build(): String {
        // Default to selecting all columns
        var selection = "*"

        // If specific attributes are selected, join them into a comma-separated string
        if (attributeSelection.isNotEmpty()) {
            selection = attributeSelection.joinToString(",")
        }

        // Build the base SQL query with the selected columns and table name
        val base = StringBuilder("SELECT $selection FROM $tableName")
        // Append the WHERE clause if any
        base.append(buildWhere())
        // Append the ORDER BY clause if any
        base.append(buildOrderBy())
        // Append the LIMIT clause if any
        base.append(buildLimit())
        // Append the OFFSET clause if any
        base.append(buildOffset())
        // Return the final query string trimmed of any extra spaces
        return base.toString().trim()
    }
}