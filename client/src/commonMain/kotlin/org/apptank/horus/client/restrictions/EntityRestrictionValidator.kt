package org.apptank.horus.client.restrictions

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.exception.OperationNotPermittedException


/**
 * The `EntityRestrictionValidator` class is responsible for validating entity-based restrictions.
 * It ensures that certain constraints (e.g., maximum allowed records for an entity) are not violated
 * during operations within the application. It interacts with a database helper to perform validation checks.

 * @param operationDatabaseHelper An instance of `IOperationDatabaseHelper` used to interact with the database for validation purposes.
 */
internal class EntityRestrictionValidator(
    private val operationDatabaseHelper: IOperationDatabaseHelper
) {
    /**
     * A list of restrictions to be validated. [EntityName -> List[EntityRestriction]]
     */
    private var entityMapRestrictions = mapOf<String, List<EntityRestriction>>()

    /**
     * A map of operations to be validated. [EntityName -> List[OperationType]]
     */
    private var queueOperations = mutableMapOf<String, List<EntityRestriction.OperationType>>()

    /**
     * A flag indicating whether the validation process has started.
     */
    private var validationStarted = false

    /**
     * A cache of count queries to avoid multiple queries for the same entity.
     */
    private val countQueryCache = mutableMapOf<String, Int>()

    /**
     * Sets the list of restrictions to be validated.
     *
     * @param restrictions A list of `EntityRestriction` objects that define the validation rules.
     */
    fun setRestrictions(restrictions: List<EntityRestriction>) {
        entityMapRestrictions = restrictions.groupBy { it.getEntityName() }
    }

    /**
     * Starts the validation process.
     */
    fun startValidation() {
        if (validationStarted) {
            throw IllegalStateException("Validation has already started.")
        }
        validationStarted = true
    }

    /**
     * Validates the restrictions for a specific entity by name.
     *
     * For each restriction in the list, it checks whether the restriction applies to the provided entity name.
     * If applicable, it performs the required validation, such as enforcing a maximum count for an entity.
     *
     * @param entityName The name of the entity to validate against the defined restrictions.
     * @throws OperationNotPermittedException if any restriction is violated.
     */
    fun validate(entityName: String, operationType: EntityRestriction.OperationType) {

        if (!validationStarted) {
            throw IllegalStateException("Validation has not been started.")
        }

        val restrictions = entityMapRestrictions[entityName] ?: return

        queueOperations[entityName] = queueOperations[entityName]?.plus(operationType) ?: listOf(operationType)

        restrictions.forEach {
            // Validate specific types of restrictions
            when (it) {
                is MaxCountEntityRestriction -> validateEntityMaxCount(it.entity, it.maxCount, operationType)
            }
        }
    }

    /**
     * Finishes the validation process.
     */
    fun finishValidation() {
        if (!validationStarted) {
            throw IllegalStateException("Validation has not been started.")
        }
        validationStarted = false
        queueOperations.clear()
        countQueryCache.clear()
    }

    /**
     * Validates the maximum count restriction for a specific entity for a given operation type.
     *
     * This method queries the database to count the records associated with the entity for the current user.
     * If the count exceeds or equals the specified maximum count, an exception is thrown.
     *
     * @param entityName The name of the entity being validated.
     * @param maxCount The maximum allowed count for the entity.
     * @param operationType The type of operation being performed.
     * @throws OperationNotPermittedException if the maximum count is exceeded.
     */
    private fun validateEntityMaxCount(entityName: String, maxCount: Int, operationType: EntityRestriction.OperationType) {

        if (operationType != EntityRestriction.OperationType.INSERT) {
            return
        }

        // Build a query to count records for the entity associated with the current user
        val queryBuilder = SimpleQueryBuilder(entityName).where(
            SQL.WhereCondition(SQL.ColumnValue(Horus.Attribute.OWNER_ID, HorusAuthentication.getEffectiveUserId()))
        )
        val countInserts = queueOperations[entityName]?.filter { it == EntityRestriction.OperationType.INSERT }?.size ?: 0
        val count = countQueryCache[entityName] ?: operationDatabaseHelper.countRecords(queryBuilder as SimpleQueryBuilder).also {
            countQueryCache[entityName] = it
        }
        val newCountExpected = count + countInserts

        // Throw an exception if the count exceeds the allowed maximum
        if (newCountExpected > maxCount) {
            throwNotPermitted("Max count of $entityName reached. (Max: $maxCount)")
        }
    }


    private fun throwNotPermitted(message: String) {
        finishValidation()
        throw OperationNotPermittedException(message)
    }
}
