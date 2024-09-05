package com.apptank.horus.client.sync.tasks.base


interface Task {
    fun getDependsOn(): Task?
    suspend fun execute(previousDataTask: Any?): TaskResult
}



abstract class BaseTask(
    private val dependsOnTask: Task? = null
) : Task {
    override fun getDependsOn(): Task? = dependsOnTask
}

sealed class TaskResult {
    data class Success(val data: Any? = null) : TaskResult()
    data class Failure(val error: Throwable) : TaskResult()

    companion object {
        fun success(data: Any? = null): TaskResult = Success(data)
        fun failure(error: Throwable): TaskResult = Failure(error)
    }
}