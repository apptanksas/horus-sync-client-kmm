package org.apptank.horus.client.tasks

import org.apptank.horus.client.bus.Event
import org.apptank.horus.client.bus.InternalEventBus
import org.apptank.horus.client.bus.EventType
import kotlin.math.round


/**
 * Interface representing a task that can be executed and has optional dependencies.
 */
internal interface Task {

    val weightPercentage: Int

    /**
     * Returns the task that this task depends on, if any.
     *
     * @return The dependent task, or `null` if there are no dependencies.
     */
    fun getDependsOn(): Task?

    /**
     * Executes the task with the given previous data and returns a result.
     *
     * @param previousDataTask Data from the previous task, if any.
     * @param weightProgressSum The sum of progress weights from previous tasks.
     * @param totalProgressWeight The total weight of all tasks for progress calculation.
     * @return [TaskResult] representing the outcome of the task execution.
     */
    suspend fun execute(previousDataTask: Any?, weightProgressSum: Int, totalProgressWeight: Int): TaskResult


    /**
     * Emits progress updates for the task execution.
     *
     * @param weightProgressSum The sum of progress weights from previous tasks.
     * @param totalWeight The total weight of all tasks for progress calculation.
     * @param taskProgress The current progress of the task as a percentage (0-100).
     */
    fun emitProgress(weightProgressSum: Int, totalWeight: Int, taskProgress: Int)
}

/**
 * Abstract base class for tasks that optionally depend on another task.
 *
 * @param dependsOnTask An optional task that this task depends on.
 */
internal abstract class BaseTask(
    private val dependsOnTask: Task? = null,
    override val weightPercentage: Int = 1
) : Task {
    /**
     * Returns the task that this task depends on.
     *
     * @return The dependent task, or `null` if there are no dependencies.
     */
    override fun getDependsOn(): Task? = dependsOnTask

    /**
     * Executes the task with the given previous data and returns a result.
     *
     * @param previousDataTask Data from the previous task, if any.
     * @param weightProgressSum The sum of progress weights from previous tasks.
     * @param totalProgressWeight The total weight of all tasks for progress calculation.
     * @return [TaskResult] representing the outcome of the task execution.
     */
    override fun emitProgress(weightProgressSum: Int, totalWeight: Int, taskProgress: Int) {
        val progress = if (taskProgress == 100) {
            // Task is completed - weightProgressSum already includes current task's weight
            val progressAfterTask = (weightProgressSum.toFloat() / totalWeight) * 100
            round(progressAfterTask).toInt()
        } else {
            // Task is in progress - weightProgressSum does NOT include current task's weight yet
            val progressBeforeTask = (weightProgressSum.toFloat() / totalWeight) * 100
            val progressAfterTask = ((weightProgressSum + weightPercentage).toFloat() / totalWeight) * 100
            round(progressBeforeTask + ((progressAfterTask - progressBeforeTask) * (taskProgress / 100.0))).toInt()
        }.coerceIn(0, 100) // Ensure progress stays between 0% and 100%

        InternalEventBus.emit(EventType.ON_PROGRESS_SYNC, Event(mapOf("progress" to progress)))
    }
}

/**
 * Sealed class representing the result of a task execution.
 */
sealed class TaskResult {
    /**
     * Represents a successful task execution.
     *
     * @param data Optional data resulting from the successful task execution.
     */
    data class Success(val data: Any? = null) : TaskResult()

    /**
     * Represents a failed task execution.
     *
     * @param error The error that caused the failure.
     */
    data class Failure(val error: Throwable) : TaskResult()

    companion object {
        /**
         * Creates a [TaskResult.Success] instance.
         *
         * @param data Optional data resulting from the successful task execution.
         * @return A [TaskResult.Success] instance.
         */
        fun success(data: Any? = null): TaskResult = Success(data)

        /**
         * Creates a [TaskResult.Failure] instance.
         *
         * @param error The error that caused the failure.
         * @return A [TaskResult.Failure] instance.
         */
        fun failure(error: Throwable): TaskResult = Failure(error)
    }
}
