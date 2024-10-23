package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.base.Callback

/**
 * Interface for the manager that handles the synchronization of uploaded files.
 */
interface ISyncFileUploadedManager {
    /**
     * Synchronizes the uploaded files.
     *
     * @param onCompleted The callback to be executed when the synchronization is completed.
     */
    fun syncFiles(onCompleted: Callback = {})
}