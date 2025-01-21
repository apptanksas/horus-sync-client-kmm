package org.apptank.horus.client.restrictions

import org.apptank.horus.client.control.helper.IOperationDatabaseHelper


class EntityRestrictionValidator(
    private val operationDatabaseHelper: IOperationDatabaseHelper
) {
    private var restrictions: List<EntityRestriction> = emptyList()


    fun setRestrictions(restrictions: List<EntityRestriction>) {
        this.restrictions = restrictions
    }

    fun validate(entityName: String) {

        restrictions.forEach {
            if (it.getEntityName() != entityName) {
                return@forEach
            }

            when (it) {
                is MaxCountEntityRestriction -> validateEntityMaxCount(it.entity, it.maxCount)
            }
        }
    }

    private fun validateEntityMaxCount(entityName: String, maxCount: Int) {
       /* val count = operationDatabaseHelper.queryRecords()
        if (count >= maxCount) {
            throw Exception("The maximum number of rows for the entity $entityName has been reached.")
        }*/
    }
}