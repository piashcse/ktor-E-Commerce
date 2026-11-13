package com.piashcse.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory

sealed class BackgroundTask {
    data class ProcessImage(val filePath: String) : BackgroundTask()
}

object AsyncWorker {
    private val log = LoggerFactory.getLogger(AsyncWorker::class.java)
    private const val QUEUE_CAPACITY = 1_000
    private var tasks = Channel<BackgroundTask>(QUEUE_CAPACITY)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        if (tasks.isClosedForSend) tasks = Channel(QUEUE_CAPACITY)
        job =
            scope.launch(Dispatchers.IO) {
                for (task in tasks) {
                    runCatching {
                        when (task) {
                            is BackgroundTask.ProcessImage -> ImageCompressor.compress(task.filePath)
                        }
                    }.onFailure { log.error("Failed to process ${task::class.simpleName}", it) }
                }
            }
    }

    fun enqueue(task: BackgroundTask): Boolean {
        if (job == null || tasks.isClosedForSend) {
            log.warn("AsyncWorker is stopped; dropping ${task::class.simpleName}")
            return false
        }
        val result = tasks.trySend(task)
        if (result.isFailure) {
            log.warn("AsyncWorker queue is full or stopped; dropping ${task::class.simpleName}")
        }
        return result.isSuccess
    }

    fun stop() {
        job?.cancel()
        job = null
        // Prevent uploads accepted after shutdown from silently accumulating in
        // an unprocessed queue. start() recreates the channel for a restart.
        tasks.close()
    }
}
