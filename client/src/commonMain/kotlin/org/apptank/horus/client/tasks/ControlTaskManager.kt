package org.apptank.horus.client.tasks

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.warn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apptank.horus.client.extensions.logException

/**
 * Manages and executes a series of tasks in a specific order, handling dependencies between tasks.
 */
internal object ControlTaskManager {

    /**
     * Enum representing the status of the task execution process.
     */
    enum class Status {
        RUNNING,     // Task execution is in progress.
        COMPLETED,   // Task execution has completed successfully.
        FAILED       // Task execution has failed.
    }


    private val networkValidator = HorusContainer.getNetworkValidator()

    // Task instances with their dependencies set up.
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

    private val retrieveDataSharedTask = RetrieveDataSharedTask(
        HorusContainer.getSettings(),
        HorusContainer.getNetworkValidator(),
        HorusContainer.getDataSharedDatabaseHelper(),
        HorusContainer.getSynchronizationService(),
        synchronizeDataTask
    )


    // The initial task to be executed at startup.
    private val startupTask = retrieveDatabaseSchemeTask

    // List of all tasks in the order they should be executed.
    private val tasks: List<Task> = listOf(
        validateHashingTask,
        retrieveDatabaseSchemeTask,
        validateMigrationLocalDatabaseTask,
        synchronizeInitialDataTask,
        synchronizeDataTask,
        retrieveDataSharedTask
    )

    // Counter for tracking the number of tasks executed.
    private var taskExecutionCounter = 0

    // Callback to be invoked on status change.
    private var onStatus: (Status) -> Unit = {}

    // Callback to be invoked when all tasks are completed.
    private var onCompleted: Callback = {}

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts the execution of tasks.
     *
     * @param dispatcher The [CoroutineDispatcher] to use for task execution. Defaults to [Dispatchers.IO].
     */
    fun start(dispatcher: CoroutineDispatcher = Dispatchers.IO) {

        if (HorusAuthentication.isNotUserAuthenticated()) {
            warn("User is not authenticated to start the horus task manager")
            onStatus(Status.FAILED)
            return
        }

        if (networkValidator.isNetworkAvailable().not()) {
            info("Network is not available to start the horus task manager")
            emitEventOnReady()
            return
        }

        taskExecutionCounter = 0

        scope.apply {
            val job = launch(dispatcher) {
                executeStartupTask()
            }
            job.invokeOnCompletion {
                if (it != null) {
                    it.printStackTrace()
                    onStatus(Status.FAILED)
                }
                job.cancel()
            }
        }
    }

    /**
     * Sets a callback to be invoked when the status of the task execution changes.
     *
     * @param callback A function to be called with the new status.
     */
    fun setOnCallbackStatusListener(callback: (Status) -> Unit) {
        onStatus = {
            callback(it)
            if (it == Status.COMPLETED) {
                onCompleted()
            }
        }
    }

    /**
     * Sets a callback to be invoked when all tasks are completed.
     *
     * @param callback A function to be called when all tasks are completed.
     */
    fun setOnCompleted(callback: Callback) {
        onCompleted = callback
    }

    /**
     * Executes the startup task.
     */
    private suspend fun executeStartupTask() {
        executeTask(startupTask, null)
    }

    /**
     * Executes a specific task and handles the result.
     *
     * @param task The task to be executed.
     * @param data Optional data to be passed to the task.
     */
    private suspend fun executeTask(task: Task, data: Any?) {
        onStatus(Status.RUNNING)
        kotlin.runCatching {
            taskExecutionCounter++
            info("[ControlTask] Executing task: ${task::class.simpleName}")
            val taskResult = task.execute(data)
            handleTaskResult(findNextTask(task), taskResult)
        }.getOrElse {
            logException("[ControlTask] Error executing task: ${task::class.simpleName}", it)
            it.printStackTrace()
            onStatus(Status.FAILED)
        }
    }

    /**
     * Finds the next task to be executed based on the current task.
     *
     * @param task The current task.
     * @return The next task to be executed, or `null` if there are no more tasks.
     */
    private fun findNextTask(task: Task): Task? {
        return tasks.find { it.getDependsOn() == task }
    }

    /**
     * Handles the result of a task execution and proceeds to the next task if applicable.
     *
     * @param nextTask The next task to be executed, if any.
     * @param taskResult The result of the current task execution.
     */
    private suspend fun handleTaskResult(nextTask: Task?, taskResult: TaskResult) {
        if (nextTask == null) {
            onStatus(Status.COMPLETED).also {
                emitEventOnReady()
            }
            return
        }

        when (taskResult) {
            is TaskResult.Success -> {
                executeTask(nextTask, taskResult.data)
            }

            is TaskResult.Failure -> {
                logException("[ControlTask] Error executing task", taskResult.error)
                onStatus(Status.FAILED)
            }
        }
    }

    /**
     * Emits an event when the task execution is completed.
     */
    private fun emitEventOnReady() {
        EventBus.emit(EventType.ON_READY)
        info("[Synchronization Validation] Horus sync is ready to operation")
    }

    /**
     * Returns the number of tasks executed so far.
     *
     * @return The task execution counter.
     */
    fun getTaskExecutionCounter(): Int {
        return taskExecutionCounter
    }
}
