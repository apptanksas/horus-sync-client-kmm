package com.apptank.horus.client.exception

/**
 * Exception thrown when an entity does not exist.
 *
 * @property entity The entity that does not exist.
 */
class EntityNotExistsException(entity: String) : HorusException("Entity $entity does not exist!")