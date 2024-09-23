package org.apptank.horus.client.tasks


/**
 * Interface representing a task that can be executed and has optional dependencies.
 */
internal interface Task {
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
     * @return [TaskResult] representing the outcome of the task execution.
     */
    suspend fun execute(previousDataTask: Any?): TaskResult
}

/**
 * Abstract base class for tasks that optionally depend on another task.
 *
 * @param dependsOnTask An optional task that this task depends on.
 */
internal abstract class BaseTask(
    private val dependsOnTask: Task? = null
) : Task {
    /**
     * Returns the task that this task depends on.
     *
     * @return The dependent task, or `null` if there are no dependencies.
     */
    override fun getDependsOn(): Task? = dependsOnTask
}

/**
 * Sealed class representing the result of a task execution.
 */
internal sealed class TaskResult {
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
