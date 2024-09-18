package com.apptank.horus.client.database.builder

import com.apptank.horus.client.database.SQL
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
                "AND (is_male = 1 OR age < 10)"
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
            )

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
}