package org.apptank.horus.client.restrictions

import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.every
import io.mockative.mock
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.exception.OperationNotPermittedException
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextUInt

class EntityRestrictionValidatorTest : TestCase() {

    @Mock
    private val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())
    private val entityRestrictionValidator = EntityRestrictionValidator(operationDatabaseHelper)


    @Before
    fun setup() {
        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
    }

    @Test(expected = OperationNotPermittedException::class)
    fun validateIsFailureWhenEntityMaxCountIsExceeded() {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        entityRestrictionValidator.validate(entityName)
    }

    @Test
    fun validateIsSuccessWhenEntityMaxCountIsNotExceeded() {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount - 1)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        entityRestrictionValidator.validate(entityName)
    }

    @Test
    fun validateIsSuccessWhenEntityIsNotHaveRestrictions() {
        val entityName = "entity"
        val maxCount = Random.nextUInt(1u, 1000u).toInt()
        val entityRestriction = MaxCountEntityRestriction(entityName, maxCount)

        every { operationDatabaseHelper.countRecords(any()) }.returns(maxCount - 1)

        // Set the restriction
        entityRestrictionValidator.setRestrictions(listOf(entityRestriction))

        // When
        entityRestrictionValidator.validate("anotherEntity")
    }
}