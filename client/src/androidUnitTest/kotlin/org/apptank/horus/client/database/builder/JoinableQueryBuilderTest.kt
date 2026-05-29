package org.apptank.horus.client.database.builder

import org.apptank.horus.client.database.struct.SQL
import org.junit.Assert
import org.junit.Test

class JoinableQueryBuilderTest {

    @Test
    fun validateBasicJoin() {
        val expected = "SELECT users.name,profiles.bio FROM users INNER JOIN profiles ON users.id = profiles.user_id"
        val builder = JoinableQueryBuilder("users")
            .select("users.name", "profiles.bio")
            .innerJoin("profiles", "users.id = profiles.user_id")

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateLeftJoin() {
        val expected = "SELECT * FROM users LEFT JOIN profiles ON users.id = profiles.user_id"
        val builder = JoinableQueryBuilder("users")
            .leftJoin("profiles", "users.id = profiles.user_id")

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateMultipleJoins() {
        val expected = "SELECT * FROM users " +
                "INNER JOIN profiles ON users.id = profiles.user_id " +
                "LEFT JOIN roles ON users.role_id = roles.id"
        val builder = JoinableQueryBuilder("users")
            .innerJoin("profiles", "users.id = profiles.user_id")
            .leftJoin("roles", "users.role_id = roles.id")

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateJoinWithWhere() {
        val expected = "SELECT users.name FROM users " +
                "INNER JOIN profiles ON users.id = profiles.user_id " +
                "WHERE users.status = 'active'"
        val builder = JoinableQueryBuilder("users")
            .select("users.name")
            .innerJoin("profiles", "users.id = profiles.user_id")
            .where(SQL.WhereCondition(SQL.ColumnValue("users.status", "active")))

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateJoinWithComplexQuery() {
        val expected = "SELECT users.name,roles.name FROM users " +
                "INNER JOIN profiles ON users.id = profiles.user_id " +
                "LEFT JOIN roles ON users.role_id = roles.id " +
                "WHERE users.status = 'active' " +
                "ORDER BY users.name ASC " +
                "LIMIT 10 OFFSET 20"
        val builder = JoinableQueryBuilder("users")
            .select("users.name", "roles.name")
            .innerJoin("profiles", "users.id = profiles.user_id")
            .leftJoin("roles", "users.role_id = roles.id")
            .where(SQL.WhereCondition(SQL.ColumnValue("users.status", "active")))
            .orderBy("users.name", SQL.OrderBy.ASC)
            .limit(10)
            .offset(20)

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }

    @Test
    fun validateGetTables() {
        val builder = JoinableQueryBuilder("users")
            .innerJoin("profiles", "users.id = profiles.user_id")
            .leftJoin("roles", "users.role_id = roles.id")

        val tables = builder.getTables()
        Assert.assertEquals(3, tables.size)
        Assert.assertTrue(tables.contains("users"))
        Assert.assertTrue(tables.contains("profiles"))
        Assert.assertTrue(tables.contains("roles"))
    }

    @Test
    fun validateSelectCountWithJoin() {
        val expected = "SELECT COUNT(*) FROM users INNER JOIN profiles ON users.id = profiles.user_id"
        val builder = JoinableQueryBuilder("users")
            .selectCount()
            .innerJoin("profiles", "users.id = profiles.user_id")

        val result = builder.build()

        Assert.assertEquals(expected, result)
    }
}
