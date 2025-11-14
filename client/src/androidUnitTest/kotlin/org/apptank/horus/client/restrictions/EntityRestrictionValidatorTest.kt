package org.apptank.horus.client.restrictions

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.cache.MemoryCache
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.scheme.EntityAttributesTable
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.OperationDatabaseHelper
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.database.struct.mapToDBColumValue
import org.apptank.horus.client.exception.OperationNotPermittedException
import org.apptank.horus.client.extensions.execute
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.assertEquals
import kotlin.test.fail

class EntityRestrictionValidatorTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var databaseHelper: OperationDatabaseHelper

    private val entityName1 = "test_entity"
    private val entityName2 = "another_entity"

    @Mock
    private val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())
    private val entityRestrictionValidator = EntityRestrictionValidator(operationDatabaseHelper)

    @Before
    fun setup() {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)

        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        databaseHelper = OperationDatabaseHelper("database", driver)

        MemoryCache.flushCache()

        driver.createTable(
            entityName1,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                Horus.Attribute.OWNER_ID to "STRING",
            )
        )

        driver.createTable(
            entityName2,
            mapOf(
                "id" to "STRING PRIMARY KEY",
                "name" to "TEXT",
                Horus.Attribute.OWNER_ID to "STRING",
            )
        )

        driver.execute(EntityAttributesTable.SQL_CREATE_TABLE)
    }

    @Test(expected = OperationNotPermittedException::class)
    fun validateIsFailureWhenEntityMaxCountIsExceeded() = runBlocking {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        with(entityRestrictionValidator) {
            startValidation(entityName)
            validate(entityName, EntityRestriction.OperationType.INSERT)
            finishValidation(entityName)
        }
    }

    @Test
    fun validateIsFailureWhenEntityMaxCountIsExceededWithMultiplesInserts() = runBlocking {
        val entityName = "entity"
        val maxCount = Random.nextUInt(500u, 1000u).toInt()
        val countInserts = Random.nextInt(9, 50)
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount + 1 - countInserts)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        try {
            with(entityRestrictionValidator) {
                startValidation(entityName)
                repeat(countInserts) {
                    validate(entityName, EntityRestriction.OperationType.INSERT)
                }
                finishValidation(entityName)
            }
            fail("Should have thrown an OperationNotPermittedException")
        } catch (_: OperationNotPermittedException) {
        }

        verify {
            operationDatabaseHelper.countRecords(any())
        }.wasInvoked(1)
    }


    @Test
    fun validateIsFailureWhenEntityMaxCountIsExceededWithMultiplesInsertsInCoroutines() = runBlocking {

        val countInserts = Random.nextInt(100, 500)
        val repeatCoroutines = 100
        val maxCount = countInserts * repeatCoroutines - 1
        val entityRestriction = MaxCountEntityRestriction(entityName1, maxCount)

        val entityRestrictionValidator = EntityRestrictionValidator(databaseHelper)
        var exceptionCalled = false

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        try {
            coroutineScope {
                repeat(repeatCoroutines) {
                    launch {
                        with(entityRestrictionValidator) {
                            startValidation(entityName1)
                            repeat(countInserts) {
                                validate(entityName1, EntityRestriction.OperationType.INSERT)
                                databaseHelper.insertWithTransaction(
                                    listOf(
                                        DatabaseOperation.InsertRecord(
                                            entityName1,
                                            mapOf(
                                                "id" to "id_${Random.nextInt()}",
                                                "name" to "name_${Random.nextInt()}",
                                                Horus.Attribute.OWNER_ID to HorusAuthentication.getEffectiveUserId(),
                                            ).map { Horus.Attribute(it.key, it.value) }.mapToDBColumValue()
                                        )
                                    )
                                )
                            }
                            finishValidation(entityName1)
                        }
                    }
                }
            }
            fail("Should have thrown an OperationNotPermittedException")
        } catch (_: OperationNotPermittedException) {
            exceptionCalled = true
        }

        assert(exceptionCalled)
    }


    @Test
    fun validateIsFailureWhenEntityMaxCountIsExceededWithMultiplesInsertsInCoroutinesWithTwoEntities() = runBlocking {

        val countInserts = Random.nextInt(100, 500)
        val repeatCoroutines = 50
        val maxCount = countInserts * repeatCoroutines + 1
        val countExpectedPerEntity = countInserts * repeatCoroutines
        val entityRestriction1 = MaxCountEntityRestriction(entityName1, maxCount-2)
        val entityRestriction2 = MaxCountEntityRestriction(entityName2, maxCount)

        val entityRestrictionValidator = EntityRestrictionValidator(databaseHelper)
        var exceptionCalled = false

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction1, entityRestriction2))

        // When
        coroutineScope {
            repeat(repeatCoroutines) {
                launch {
                    try {

                        delay(2)

                        with(entityRestrictionValidator) {
                            startValidation(entityName1)
                            repeat(countInserts) {
                                validate(entityName1, EntityRestriction.OperationType.INSERT)
                                databaseHelper.insertWithTransaction(
                                    listOf(
                                        DatabaseOperation.InsertRecord(
                                            entityName1,
                                            mapOf(
                                                "id" to "id_${Random.nextInt()}",
                                                "name" to "name_${Random.nextInt()}",
                                                Horus.Attribute.OWNER_ID to HorusAuthentication.getEffectiveUserId(),
                                            ).map { Horus.Attribute(it.key, it.value) }.mapToDBColumValue()
                                        )
                                    )
                                )
                            }
                            finishValidation(entityName1)
                        }
                    }catch (_: OperationNotPermittedException) {
                        exceptionCalled = true
                    }
                }

                launch {

                    with(entityRestrictionValidator) {
                        startValidation(entityName2)
                        repeat(countInserts) {
                            validate(entityName2, EntityRestriction.OperationType.INSERT)
                            databaseHelper.insertWithTransaction(
                                listOf(
                                    DatabaseOperation.InsertRecord(
                                        entityName2,
                                        mapOf(
                                            "id" to "id_${Random.nextInt()}",
                                            "name" to "name_${Random.nextInt()}",
                                            Horus.Attribute.OWNER_ID to HorusAuthentication.getEffectiveUserId(),
                                        ).map { Horus.Attribute(it.key, it.value) }.mapToDBColumValue()
                                    )
                                )
                            )
                        }
                        finishValidation(entityName2)
                    }
                }
            }
        }

        // Then
        assert(exceptionCalled)
        assertEquals(countExpectedPerEntity, databaseHelper.countRecords(SimpleQueryBuilder(entityName2)))
    }

    @Test
    fun validateIsSuccessWhenEntityMaxCountIsNotExceeded() = runBlocking {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount - 2)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        with(entityRestrictionValidator) {
            startValidation(entityName)
            validate(entityName, EntityRestriction.OperationType.INSERT)
            finishValidation(entityName)
        }
    }

    @Test
    fun validateIsSuccessWhenEntityMaxCountIsReached() = runBlocking {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val countInserts = Random.nextInt(1, 10)
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount - countInserts)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        with(entityRestrictionValidator) {
            startValidation(entityName)
            repeat(countInserts) {
                validate(entityName, EntityRestriction.OperationType.INSERT)
            }
            finishValidation(entityName)
        }
    }

    @Test
    fun validateIsSuccessWhenEntityIsNotHaveRestrictions(): Unit = runBlocking {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount - 1)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        with(entityRestrictionValidator) {
            launch {
                startValidation(entityName)
                validate(entityName, EntityRestriction.OperationType.INSERT)
                finishValidation(entityName)
            }
            launch {
                startValidation("otherEntity")
                validate("otherEntity", EntityRestriction.OperationType.INSERT)
                finishValidation("otherEntity")
            }
        }
    }

    @Test(timeout = 10000L)
    fun validateValidationMutex() = runBlocking {
        coroutineScope {
            repeat(10) {
                entityRestrictionValidator.startValidation(entityName1)
                delay(10)
                entityRestrictionValidator.finishValidation(entityName1)
            }
        }
    }

    @Test(timeout = 10000L)
    fun validateValidationMutexWithCoroutines() = runBlocking {
        coroutineScope {
            repeat(10) {
                launch {
                    entityRestrictionValidator.startValidation(entityName1)
                    delay(10)
                    entityRestrictionValidator.finishValidation(entityName1)
                }
            }
        }
    }


    @Test(timeout = 2000L)
    fun validatePerformanceMultiplesCoroutines() = runBlocking {
        val entityName = "entity"
        val maxCount = 100000
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)
        val startTime = System.currentTimeMillis()

        every { operationDatabaseHelper.countRecords(any()) }.returns(0)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        coroutineScope {
            repeat(5000) {
                launch {
                    with(entityRestrictionValidator) {
                        startValidation(entityName)
                        repeat(1000) {
                            validate(entityName, EntityRestriction.OperationType.INSERT)
                        }
                        finishValidation(entityName)
                    }
                }
            }
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        assert(duration < 2000L)
    }
}