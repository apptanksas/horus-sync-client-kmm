package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.database.struct.toUpdateRecord
import org.apptank.horus.client.exception.UserNotAuthenticatedException
import org.apptank.horus.client.extensions.log

abstract class BaseSynchronizator(
    protected val operationDatabaseHelper: IOperationDatabaseHelper
) {

    /**
     * Retrieves an entity by its ID.
     *
     * @param entity The name of the entity.
     * @param id The ID of the entity.
     * @return The entity if found, `null` otherwise.
     */
    protected fun getEntityById(entity: String, id: String): Horus.Entity? {

        val queryBuilder = SimpleQueryBuilder(entity).apply {
            where(
                SQL.WhereCondition(
                    SQL.ColumnValue(Horus.Attribute.ID, id)
                )
            )
        }

        return operationDatabaseHelper.queryRecords(queryBuilder).map {
            Horus.Entity(
                entity,
                it.map { Horus.Attribute(it.key, it.value) }
            )
        }.firstOrNull()
    }

    /**
     * Retrieves the authenticated user ID.
     *
     * @return The authenticated user ID.
     * @throws UserNotAuthenticatedException If the user is not authenticated.
     */
    protected fun getUserId(): String {
        return HorusAuthentication.getUserAuthenticatedId() ?: throw UserNotAuthenticatedException()
    }

    /**
     * Maps a synchronization action to a database update operation.
     *
     * @param action The synchronization action.
     * @return The database update operation if successful, `null` otherwise.
     */
    protected fun mapActionToUpdateDatabaseOperation(action: SyncControl.Action): DatabaseOperation.UpdateRecord? {
        return getEntityById(action.entity, action.getEntityId())?.let { entity ->
            action.toUpdateRecord(entity)
        } ?: run {
            log("[SynchronizatorManager] Error updating data. [${action.data}]")
            null
        }
    }

    
}