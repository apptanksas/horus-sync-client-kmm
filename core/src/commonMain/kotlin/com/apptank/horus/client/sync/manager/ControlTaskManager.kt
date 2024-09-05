package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.sync.tasks.RetrieveDatabaseSchemeTask
import com.apptank.horus.client.sync.tasks.base.MigrateLocalDatabaseTask
import com.apptank.horus.client.sync.tasks.base.Task
import com.apptank.horus.client.sync.tasks.base.TaskResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

object ControlTaskManager {

    enum class Status {
        RUNNING,
        COMPLETED,
        FAILED
    }

    private val retrieveDatabaseSchemeTask =
        RetrieveDatabaseSchemeTask(HorusContainer.getMigrationService())
    private val migrateLocalDatabaseTask =
        MigrateLocalDatabaseTask(HorusContainer.getDatabaseFactory(), retrieveDatabaseSchemeTask)

    private val startupTask = retrieveDatabaseSchemeTask

    private val tasks: List<Task> = listOf(
        retrieveDatabaseSchemeTask,
        migrateLocalDatabaseTask
    )

    private var onStatus: (Status) -> Unit = {}

    fun start(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
        CoroutineScope(dispatcher).launch {
            executeStartupTask()
        }
    }

    fun setOnCallbackStatusListener(callback: (Status) -> Unit) {
        onStatus = callback
    }

    private suspend fun executeStartupTask() {
        executeTask(startupTask, null)
    }

    private suspend fun executeTask(task: Task, data: Any?) {
        onStatus(Status.RUNNING)
        val taskResult = task.execute(data)
        handleTaskResult(findNextTask(task), taskResult)
    }

    private fun findNextTask(task: Task): Task? {
        return tasks.find { it.getDependsOn() == task }
    }

    private suspend fun handleTaskResult(nextTask: Task?, taskResult: TaskResult) {

        if (nextTask == null) {
            return onStatus(Status.COMPLETED)
        }

        when (taskResult) {
            is TaskResult.Success -> {
                executeTask(nextTask, taskResult.data)
            }

            is TaskResult.Error -> {
                onStatus(Status.FAILED)
            }
        }
    }


}