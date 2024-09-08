package com.apptank.horus.client.data

import com.apptank.horus.client.base.DataMap

interface DataChangeListener {
    fun onInsert(entity: String, id: String, data: DataMap)
    fun onUpdate(entity: String, id: String, data: DataMap)
    fun onDelete(entity: String, id: String)
}