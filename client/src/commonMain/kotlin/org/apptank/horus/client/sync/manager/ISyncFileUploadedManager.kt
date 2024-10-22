package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.base.Callback

interface ISyncFileUploadedManager{
    fun syncFiles(onCompleted: Callback = {})
}