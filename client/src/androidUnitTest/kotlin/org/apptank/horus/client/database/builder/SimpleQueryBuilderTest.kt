package org.apptank.horus.client.database.builder

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.apptank.horus.client.database.struct.SQL
import org.junit.Assert
import org.junit.Test


class SimpleQueryBuilderTest {

    @Test
    fun validateGetAllData() {
        val expected = "SELECT * FROM category"
        val builder = SimpleQueryBuilder("category")
        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateGetAllDataWithSelection() {
        val expected = "SELECT id,date,value FROM category"
        val builder = SimpleQueryBuilder("category")

        builder.select("id", "date", "value")

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWithSimpleWhereAnd() {
        val expected = "SELECT * FROM category WHERE id > 120 AND date = '2023'"
        val builder = SimpleQueryBuilder("category")

        builder.where(
            SQL.WhereCondition(
                SQL.ColumnValue(
                    "id",
                    120
                ), SQL.Comparator.GREATER_THAN
            ),
            SQL.WhereCondition(
                SQL.ColumnValue(
                    "date",
                    "2023"
                )
            )
        )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWithWhereAndJoinOr() {
        val expected = "SELECT * FROM category WHERE (id > 120 AND date = '2023') " +
                "OR (is_male = 1 AND age < 10)"
        val builder = SimpleQueryBuilder("category")

        builder
            .where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "id",
                        120
                    ), SQL.Comparator.GREATER_THAN
                ),
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "date",
                        "2023"
                    )
                )
            ).where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "is_male",
                        true
                    )
                ),
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "age",
                        10
                    ), SQL.Comparator.LESS_THAN
                ),
                joinOperator = SQL.LogicOperator.OR
            )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWithWhereAndJoinOrGrouped() {
        val expected = "SELECT * FROM category WHERE (id > 120 AND date = '2023') " +
                "AND (is_male = 1 OR age < 10) AND (type IN ('A','B'))"
        val builder = SimpleQueryBuilder("category")

        builder
            .where(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "id",
                        120
                    ), SQL.Comparator.GREATER_THAN
                ),
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "date",
                        "2023"
                    )
                )
            ).whereOr(
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "is_male",
                        true
                    ),
                ),
                SQL.WhereCondition(
                    SQL.ColumnValue(
                        "age",
                        10
                    ), SQL.Comparator.LESS_THAN
                ),
            ).whereIn(
                "type",
                listOf("A", "B")
            )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereInInteger() {
        val expected = "SELECT * FROM category WHERE id IN (1,2,3)"
        val builder = SimpleQueryBuilder("category")

        builder.whereIn("id", listOf(1, 2, 3))

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereInString() {
        val expected = "SELECT * FROM category WHERE name IN ('John','Doe')"
        val builder = SimpleQueryBuilder("category")

        builder.whereIn("name", listOf("John", "Doe"))

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }


    @Test
    fun validateLimit() {
        val expected = "SELECT * FROM category LIMIT 10"
        val builder = SimpleQueryBuilder("category")

        val result = builder.limit(10).build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateOrderByAge() {
        val expected = "SELECT * FROM category ORDER BY age DESC"
        val builder = SimpleQueryBuilder("category")

        val result = builder.orderBy("age").build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateLimitAndOrderBy() {
        val expected = "SELECT * FROM category ORDER BY age DESC LIMIT 10"
        val builder = SimpleQueryBuilder("category")

        val result = builder.limit(10).orderBy("age").build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateSelectCount() {
        val expected = "SELECT COUNT(*) FROM category"
        val builder = SimpleQueryBuilder("category")

        val result = builder.selectCount().build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereLike() {
        val expected = "SELECT * FROM category WHERE name LIKE '%John%'"
        val builder = SimpleQueryBuilder("category")

        builder.where(SQL.WhereCondition(SQL.ColumnValue("name", "%John%"), SQL.Comparator.LIKE))

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateOrderByMultipleColumns() {
        val expected = "SELECT * FROM category ORDER BY age DESC, name ASC"
        val builder = SimpleQueryBuilder("category")

        val result = builder.orderBy("age", SQL.OrderBy.DESC)
            .orderBy("name", SQL.OrderBy.ASC).build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereIsNull() {
        val expected = "SELECT * FROM category WHERE name IS NULL"
        val builder = SimpleQueryBuilder("category")

        builder.where(SQL.WhereCondition(SQL.ColumnValue("name", null), SQL.Comparator.IS_NULL))

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereIsNotNull() {
        val expected = "SELECT * FROM category WHERE name IS NOT NULL"
        val builder = SimpleQueryBuilder("category")

        builder.where(SQL.WhereCondition(SQL.ColumnValue("name", null), SQL.Comparator.IS_NOT_NULL))

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereWithInstant() {
        val currentDate = Clock.System.now()
        val expected = "SELECT * FROM category WHERE created_at = ${currentDate.epochSeconds}"
        val builder = SimpleQueryBuilder("category")

        builder.where(
            SQL.WhereCondition(
                SQL.ColumnValue(
                    "created_at",
                    currentDate
                )
            )
        )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereWithLocalDate() {
        val currentDate = LocalDate(2023, 10, 1)
        val instant = currentDate.atTime(12, 0, 0).toInstant(TimeZone.UTC)
        val expected = "SELECT * FROM category WHERE created_at = ${instant.epochSeconds}"
        val builder = SimpleQueryBuilder("category")

        builder.where(
            SQL.WhereCondition(
                SQL.ColumnValue(
                    "created_at",
                    currentDate
                )
            )
        )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWhereWithLocalDateTime(){
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val instant = currentDate.toInstant(TimeZone.UTC)
        val expected = "SELECT * FROM category WHERE created_at = ${instant.epochSeconds}"
        val builder = SimpleQueryBuilder("category")

        builder.where(
            SQL.WhereCondition(
                SQL.ColumnValue(
                    "created_at",
                    currentDate
                )
            )
        )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }
}