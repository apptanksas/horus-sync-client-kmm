package com.apptank.horus.client.database.builder

import com.apptank.horus.client.database.LocalDatabase
import org.junit.Assert
import org.junit.Test


class SimpleQueryBuilderTest {

    @Test
    fun validateGetAllData() {
        val expected = "SELECT * FROM animal"
        val builder = SimpleQueryBuilder("animal")
        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateGetAllDataWithSelection() {
        val expected = "SELECT id,date,value FROM animal"
        val builder = SimpleQueryBuilder("animal")

        builder.select("id", "date", "value")

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWithSimpleWhereAnd() {
        val expected = "SELECT * FROM animal WHERE id > 120 AND date = '2023'"
        val builder = SimpleQueryBuilder("animal")

        builder.where(
            LocalDatabase.WhereCondition(
                LocalDatabase.ColumnValue(
                    "id",
                    120
                ), ">"
            ),
            LocalDatabase.WhereCondition(
                LocalDatabase.ColumnValue(
                    "date",
                    "2023"
                ), "="
            )
        )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWithWhereAndJoinOr() {
        val expected = "SELECT * FROM animal WHERE (id > 120 AND date = '2023') " +
                "OR (is_male = 1 AND age < 10)"
        val builder = SimpleQueryBuilder("animal")

        builder
            .where(
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "id",
                        120
                    ), ">"
                ),
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "date",
                        "2023"
                    ), "="
                )
            ).where(
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "is_male",
                        true
                    ), "="
                ),
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "age",
                        10
                    ), "<"
                ),
                joinOperator = LocalDatabase.OperatorComparator.OR
            )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateWithWhereAndJoinOrGrouped() {
        val expected = "SELECT * FROM animal WHERE (id > 120 AND date = '2023') " +
                "AND (is_male = 1 OR age < 10)"
        val builder = SimpleQueryBuilder("animal")

        builder
            .where(
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "id",
                        120
                    ), ">"
                ),
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "date",
                        "2023"
                    ), "="
                )
            ).whereOr(
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "is_male",
                        true
                    ), "="
                ),
                LocalDatabase.WhereCondition(
                    LocalDatabase.ColumnValue(
                        "age",
                        10
                    ), "<"
                ),
            )

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }


    @Test
    fun validateLimit() {
        val expected = "SELECT * FROM animal LIMIT 10"
        val builder = SimpleQueryBuilder("animal")

        val result = builder.limit(10).build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateOrderByAge() {
        val expected = "SELECT * FROM animal ORDER BY age DESC"
        val builder = SimpleQueryBuilder("animal")

        val result = builder.orderBy("age").build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateLimitAndOrderBy() {
        val expected = "SELECT * FROM animal ORDER BY age DESC LIMIT 10"
        val builder = SimpleQueryBuilder("animal")

        val result = builder.limit(10).orderBy("age").build()

        Assert.assertEquals(expected, result)
    }
}