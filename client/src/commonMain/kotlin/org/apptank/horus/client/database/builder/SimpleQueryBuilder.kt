package org.apptank.horus.client.database.builder

import org.apptank.horus.client.database.struct.SQL
import kotlin.math.cos

class SimpleQueryBuilder(
    private val tableName: String
) : QueryBuilder() {

    private var selectCount = false

    init {
        if (tableName.isEmpty()) {
            throw IllegalArgumentException("tableName cannot be empty")
        }
    }

    fun selectCount(): SimpleQueryBuilder {
        selectCount = true
        return this
    }

    override fun getTables(): List<String> {
        return listOf(tableName)
    }

    override fun build(): String {
        // Default to selecting all columns
        var selection = "*"

        // If specific attributes are selected, join them into a comma-separated string
        if (attributeSelection.isNotEmpty()) {
            selection = attributeSelection.joinToString(",")
        }

        if (selectCount) {
            selection = "COUNT(*)"
        }

        // Build the base SQL query with the selected columns and table name
        val base = StringBuilder("SELECT $selection FROM $tableName")
        // Append the WHERE clause if any
        base.append(buildWhere())
        // Append the coordinate extension filter if any
        base.append(buildCoordinateExtension())
        // Append the ORDER BY clause if any
        base.append(buildOrderBy())
        // Append the LIMIT clause if any
        base.append(buildLimit())
        // Append the OFFSET clause if any
        base.append(buildOffset())
        // Return the final query string trimmed of any extra spaces
        return base.toString().trim()
    }

    /**
     * Builds the coordinate extension filter for the query.
     * Uses a bounding box approximation for filtering points within a distance.
     *
     * The formula uses the fact that:
     * - 1 degree of latitude ≈ 111.32 km
     * - 1 degree of longitude ≈ 111.32 * cos(latitude) km
     *
     * @return The WHERE clause fragment for coordinate filtering.
     */
    private fun buildCoordinateExtension(): String {
        val coordinateExtension = getExtensions().filterIsInstance<SQL.Coordinates.WithIn>().firstOrNull() ?: return ""

        val column = coordinateExtension.column
        val refLat = coordinateExtension.point.latitude
        val refLon = coordinateExtension.point.longitude
        val distanceKm = coordinateExtension.distanceInKm

        // Earth's radius in km
        val earthRadiusKm = 6371.0

        // Calculate the delta for latitude and longitude based on distance
        val deltaLat = distanceKm / earthRadiusKm * (180.0 / kotlin.math.PI)
        val refLatRadians = refLat * kotlin.math.PI / 180.0
        val deltaLon = distanceKm / (earthRadiusKm * cos(refLatRadians)) * (180.0 / kotlin.math.PI)

        // Calculate bounding box
        val minLat = refLat - deltaLat
        val maxLat = refLat + deltaLat
        val minLon = refLon - deltaLon
        val maxLon = refLon + deltaLon

        // SQL expressions to extract latitude and longitude from "lat,lon" format
        val latExpr = "CAST(SUBSTR($column, 1, INSTR($column, ',') - 1) AS REAL)"
        val lonExpr = "CAST(SUBSTR($column, INSTR($column, ',') + 1) AS REAL)"

        // Build the WHERE clause using bounding box
        val wherePrefix = if (buildWhere().isEmpty()) " WHERE " else " AND "

        return "$wherePrefix$latExpr >= $minLat AND $latExpr <= $maxLat AND $lonExpr >= $minLon AND $lonExpr <= $maxLon"
    }

    fun getTableName(): String {
        return tableName
    }
}