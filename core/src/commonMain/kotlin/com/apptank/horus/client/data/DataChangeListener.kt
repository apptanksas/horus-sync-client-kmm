package com.apptank.horus.client.data

interface DataChangeListener {
    fun onInsert(entity:String, id: String)
    fun onUpdate(entity:String, id: String)
    fun onDelete(entity:String, id: String)
}