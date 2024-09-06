package com.apptank.horus.client.tasks

import com.apptank.horus.client.di.HorusContainer
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

    private val validateHashingTask = ValidateHashingTask(
        HorusContainer.getSyncControlDatabaseHelper(),
        HorusContainer.getSynchronizationService()
    )

    private val retrieveDatabaseSchemeTask = RetrieveDatabaseSchemeTask(
        HorusContainer.getMigrationService()
    )

    private val validateMigrationLocalDatabaseTask = ValidateMigrationLocalDatabaseTask(
        HorusContainer.getSettings(),
        HorusContainer.getDatabaseFactory(),
        HorusContainer.getSyncControlDatabaseHelper(),
        retrieveDatabaseSchemeTask
    )

    private val synchronizeInitialDataTask = SynchronizeInitialDataTask(
        HorusContainer.getOperationDatabaseHelper(),
        HorusContainer.getSyncControlDatabaseHelper(),
        HorusContainer.getSynchronizationService(),
        validateMigrationLocalDatabaseTask
    )

    //private val synchronizeDataTask = SynchronizeDataTask()


    private val startupTask = retrieveDatabaseSchemeTask

    private val tasks: List<Task> = listOf(
        retrieveDatabaseSchemeTask,
        validateMigrationLocalDatabaseTask
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
        kotlin.runCatching {
            val taskResult = task.execute(data)
            handleTaskResult(findNextTask(task), taskResult)
        }.getOrElse {
            onStatus(Status.FAILED)
        }
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

            is TaskResult.Failure -> {
                onStatus(Status.FAILED)
            }
        }
    }


}