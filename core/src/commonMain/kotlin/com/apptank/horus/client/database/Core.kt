package com.apptank.horus.client.database

data class Column(
    val position: Int,
    val name: String,
    val type: String,
    val nullable: Boolean,
)


data class Cursor(
    val index: Int,
    val columns: List<Column>
)

data class CursorValue<T>(
    val value: T,
    val column: Column
)