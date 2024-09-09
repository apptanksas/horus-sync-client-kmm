package com.apptank.horus.client.tasks

import com.apptank.horus.client.base.Callback
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
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

    private val retrieveDatabaseSchemeTask = RetrieveDatabaseSchemeTask(
        HorusContainer.getMigrationService()
    )

    private val validateMigrationLocalDatabaseTask = ValidateMigrationLocalDatabaseTask(
        HorusContainer.getSettings(),
        HorusContainer.getDatabaseFactory(),
        retrieveDatabaseSchemeTask
    )

    private val validateHashingTask = ValidateHashingTask(
        HorusContainer.getSyncControlDatabaseHelper(),
        HorusContainer.getSynchronizationService(),
        validateMigrationLocalDatabaseTask
    )


    private val synchronizeInitialDataTask = SynchronizeInitialDataTask(
        HorusContainer.getNetworkValidator(),
        HorusContainer.getOperationDatabaseHelper(),
        HorusContainer.getSyncControlDatabaseHelper(),
        HorusContainer.getSynchronizationService(),
        validateHashingTask
    )

    private val synchronizeDataTask = SynchronizeDataTask(
        HorusContainer.getNetworkValidator(),
        HorusContainer.getSyncControlDatabaseHelper(),
        HorusContainer.getOperationDatabaseHelper(),
        HorusContainer.getSynchronizationService(),
        synchronizeInitialDataTask
    )


    private val startupTask = retrieveDatabaseSchemeTask

    private val tasks: List<Task> = listOf(
        validateHashingTask,
        retrieveDatabaseSchemeTask,
        validateMigrationLocalDatabaseTask,
        synchronizeInitialDataTask,
        synchronizeDataTask
    )

    private var taskExecutionCounter = 0

    private var onStatus: (Status) -> Unit = {}
    private var onCompleted: Callback = {}

    fun start(dispatcher: CoroutineDispatcher = Dispatchers.IO) {

        taskExecutionCounter = 0

        CoroutineScope(dispatcher).launch {
            executeStartupTask()
        }.invokeOnCompletion {
            if (it != null) {
                it.printStackTrace()
                onStatus(Status.FAILED)
            }
        }
    }

    fun setOnCallbackStatusListener(callback: (Status) -> Unit) {
        onStatus = {
            callback(it)
            if (it == Status.COMPLETED) {
                onCompleted()
            }
        }
    }

    fun setOnCompleted(callback: Callback) {
        onCompleted = callback
    }

    private suspend fun executeStartupTask() {
        executeTask(startupTask, null)
    }

    private suspend fun executeTask(task: Task, data: Any?) {
        onStatus(Status.RUNNING)
        kotlin.runCatching {
            taskExecutionCounter++
            val taskResult = task.execute(data)
            handleTaskResult(findNextTask(task), taskResult)
        }.getOrElse {
            it.printStackTrace()
            onStatus(Status.FAILED)
        }
    }

    private fun findNextTask(task: Task): Task? {
        return tasks.find { it.getDependsOn() == task }
    }

    private suspend fun handleTaskResult(nextTask: Task?, taskResult: TaskResult) {

        if (nextTask == null) {
            return onStatus(Status.COMPLETED).also {
                emitEventOnReady()
            }
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

    private fun emitEventOnReady() {
        EventBus.post(EventType.VALIDATION_COMPLETED)
    }

    fun getTaskExecutionCounter(): Int {
        return taskExecutionCounter
    }
}