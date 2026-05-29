package org.apptank.horus.client.database.builder

import org.apptank.horus.client.database.struct.SQL
import org.junit.Assert
import org.junit.Test

class UnionQueryBuilderTest {

    @Test
    fun validateUserExample() {
        val expected = "SELECT EXISTS(" +
                "SELECT 1 FROM table1 WHERE animal_id = 1 " +
                "UNION ALL " +
                "SELECT 1 FROM table2 WHERE animal_id = 1 " +
                "UNION ALL " +
                "SELECT 1 FROM table3 WHERE animal_id = 1" +
                ")"
        
        val condition = SQL.WhereCondition(SQL.ColumnValue("animal_id", 1))
        
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder("table1").select("1").where(condition))
            .add(SimpleQueryBuilder("table2").select("1").where(condition))
            .add(SimpleQueryBuilder("table3").select("1").where(condition))
            .unionAll()
            .asExists()

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateSimpleUnion() {
        val expected = "SELECT * FROM table1 UNION SELECT * FROM table2"
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder("table1"))
            .add(SimpleQueryBuilder("table2"))
            .unionAll(false)

        val result = builder.build()
        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateUnionWithOrderByAndLimit() {
        val expected = "SELECT name FROM users UNION ALL SELECT name FROM admins ORDER BY name ASC LIMIT 10"
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder("users").select("name"))
            .add(SimpleQueryBuilder("admins").select("name"))
            .orderBy("name", SQL.OrderBy.ASC)
            .limit(10)

        val result = builder.build()
        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateGetTables() {
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder("table1"))
            .add(JoinableQueryBuilder("table2").innerJoin("table3", "table2.id = table3.id"))

        val tables = builder.getTables()
        Assert.assertEquals(3, tables.size)
        Assert.assertTrue(tables.contains("table1"))
        Assert.assertTrue(tables.contains("table2"))
        Assert.assertTrue(tables.contains("table3"))
    }

    @Test
    fun validateSelectCount() {
        val expected = "SELECT COUNT(*) FROM (SELECT * FROM table1 UNION ALL SELECT * FROM table2)"
        val builder = UnionQueryBuilder()
            .add(SimpleQueryBuilder("table1"))
            .add(SimpleQueryBuilder("table2"))
            .selectCount()

        val result = builder.build()
        Assert.assertEquals(expected, result)
    }
}
