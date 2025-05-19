package org.apptank.horus.client.restrictions

import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.exception.OperationNotPermittedException
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.fail

class EntityRestrictionValidatorTest : TestCase() {

    @Mock
    private val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())
    private val entityRestrictionValidator = EntityRestrictionValidator(operationDatabaseHelper)


    @Before
    fun setup() {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
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
            startValidation()
            validate(entityName, EntityRestriction.OperationType.INSERT)
            finishValidation()
        }
    }

    @Test
    fun validateIsFailureWhenEntityMaxCountIsExceededWithMultiplesInserts() = runBlocking {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val countInserts = Random.nextInt(1, 10)
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount + 1 - countInserts)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        try {
            with(entityRestrictionValidator) {
                startValidation()
                repeat(countInserts) {
                    validate(entityName, EntityRestriction.OperationType.INSERT)
                }
                finishValidation()
            }
            fail("Should have thrown an OperationNotPermittedException")
        } catch (_: OperationNotPermittedException) {
        }

        verify {
            operationDatabaseHelper.countRecords(any())
        }.wasInvoked(1)
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
            startValidation()
            validate(entityName, EntityRestriction.OperationType.INSERT)
            finishValidation()
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
            startValidation()
            repeat(countInserts) {
                validate(entityName, EntityRestriction.OperationType.INSERT)
            }
            finishValidation()
        }
    }

    @Test
    fun validateIsSuccessWhenEntityIsNotHaveRestrictions() = runBlocking {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount - 1)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        with(entityRestrictionValidator) {
            startValidation()
            validate("anotherEntity", EntityRestriction.OperationType.INSERT)
            finishValidation()
        }
    }

    @Test(timeout = 10000L)
    fun validateValidationMutex() = runBlocking {
        coroutineScope {
            repeat(10) {
                entityRestrictionValidator.startValidation()
                delay(10)
                entityRestrictionValidator.finishValidation()
            }
        }
    }
}