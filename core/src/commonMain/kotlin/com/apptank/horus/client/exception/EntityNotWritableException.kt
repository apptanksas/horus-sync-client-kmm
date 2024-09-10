package com.apptank.horus.client.exception

/**
 * Exception thrown when an entity is not writable.
 *
 * @property entity The entity that is not writable.
 */
class EntityNotWritableException(entity: String) : HorusException("Entity $entity is not writable")