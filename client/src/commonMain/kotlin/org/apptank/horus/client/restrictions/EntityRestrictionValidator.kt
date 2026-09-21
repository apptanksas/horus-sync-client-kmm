package org.apptank.horus.client.restrictions

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.exception.OperationNotPermittedException
import org.apptank.horus.client.extensions.isTrue


/**
 * The `EntityRestrictionValidator` class is responsible for validating entity-based restrictions.
 * It ensures that certain constraints (e.g., maximum allowed records for an entity) are not violated
 * during operations within the application. It interacts with a database helper to perform validation checks.

 * @param operationDatabaseHelper An instance of `IOperationDatabaseHelper` used to interact with the database for validation purposes.
 */
internal class EntityRestrictionValidator(
    private val operationDatabaseHelper: IOperationDatabaseHelper
) {
    private val entityMutexes = mutableMapOf<String, Mutex>()
    private val mapMutex = Mutex()

    /** Map of restrictions grouped by entity name. */
    private var entityMapRestrictions = mapOf<String, List<EntityRestriction>>()

    /** Pending INSERT operations tracked per entity (avoids storing lists). */
    private val pendingInsertsByEntity = mutableMapOf<String, Int>()

    /** Initial count for each entity, loaded once at startValidation(). */
    private val initialCountByEntity = mutableMapOf<String, Int>()

    /** Indicates if the validation session is active. */
    private var validationStarted = mutableMapOf<String, Boolean>()

    private suspend fun getEntityMutex(entityName: String): Mutex {
        return mapMutex.withLock {
            entityMutexes.getOrPut(entityName) { Mutex() }
        }
    }

    fun setRestrictions(restrictions: List<EntityRestriction>) {
        entityMapRestrictions = restrictions.groupBy { it.getEntityName() }
    }


    /**
     * Starts the validation session.
     *
     * Loads the initial counts for each entity to be validated.
     * @throws IllegalStateException if validation has already started.
     * @param entityName The name of the entity for which to start validation.
     * */
    suspend fun startValidation(entityName: String) {

        val entityMutex = getEntityMutex(entityName)
        entityMutex.lock()

        if (validationStarted[entityName] == true) {
            entityMutex.unlock()
            throw IllegalStateException("Validation has already started.")
        }

        validationStarted[entityName] = true

        val queryBuilder = SimpleQueryBuilder(entityName).where(
            SQL.WhereCondition(
                SQL.ColumnValue(
                    Horus.Attribute.OWNER_ID,
                    HorusAuthentication.getEffectiveUserId()
                )
            )
        )
        val count = operationDatabaseHelper.countRecords(queryBuilder)

        mapMutex.withLock {
            initialCountByEntity[entityName] = count
            // Initialize pending counters
            pendingInsertsByEntity[entityName] = 0
        }

    }


    /**
     * Validates an operation for a given entity.
     *
     * This implementation is optimized to operate in O(1):
     *   - No list allocations
     *   - No filtering or scanning existing operations
     *   - Only integer increments and a simple comparison
     */
    suspend fun validate(entityName: String, operationType: EntityRestriction.OperationType) {

        val isStarted = mapMutex.withLock { validationStarted[entityName] == true }

        if (!isStarted) {
            finishValidation(entityName)
            throw IllegalStateException("Validation has not been started.")
        }

        val restrictions = entityMapRestrictions[entityName] ?: return

        restrictions.forEach { restriction ->
            when (restriction) {
                is MaxCountEntityRestriction -> {
                    // Only INSERT operations are relevant for max-count rules
                    if (operationType == EntityRestriction.OperationType.INSERT) {

                        val (initial, pending) = mapMutex.withLock {
                            val i = initialCountByEntity[entityName] ?: 0
                            val p = pendingInsertsByEntity[entityName] ?: 0
                            i to p
                        }

                        val newPending = pending + 1
                        val expectedTotal = initial + newPending

                        if (expectedTotal > restriction.maxCount) {
                            finishValidation(entityName)
                            throw OperationNotPermittedException(
                                "Max count of $entityName reached. (Max: ${restriction.maxCount})"
                            )
                        }

                        // Confirm the pending increment
                        mapMutex.withLock {
                            pendingInsertsByEntity[entityName] = newPending
                        }
                    }
                }
            }
        }
    }


    /**
     * Finalizes the validation session.
     *
     * Releases all internal state so a new session can start cleanly.
     *
     * @throws IllegalStateException if validation has not been started.
     * @param entityName The name of the entity for which to finish validation.
     */
    suspend fun finishValidation(entityName: String) {

        val isStarted = mapMutex.withLock {
            val started = validationStarted[entityName] == true
            if (started) {
                validationStarted[entityName] = false
                pendingInsertsByEntity[entityName] = 0
                initialCountByEntity[entityName] = 0
            }
            started
        }

        if (!isStarted) {
            throw IllegalStateException("Validation has not been started.")
        }

        val entityMutex = getEntityMutex(entityName)
        if (entityMutex.isLocked) {
            entityMutex.unlock()
        }
    }
}

