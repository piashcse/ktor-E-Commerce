package com.piashcse.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory

sealed class BackgroundTask {
    data class ProcessImage(val filePath: String) : BackgroundTask()
}

object AsyncWorker {
    private val log = LoggerFactory.getLogger(AsyncWorker::class.java)
    private val tasks = Channel<BackgroundTask>(Channel.UNLIMITED)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            for (task in tasks) {
                runCatching {
                    when (task) {
                        is BackgroundTask.ProcessImage -> ImageCompressor.compress(task.filePath)
                    }
                }.onFailure { log.error("Failed to process ${task::class.simpleName}", it) }
            }
        }
    }

    fun enqueue(task: BackgroundTask) {
        tasks.trySend(task)
    }

    fun stop() {
        job?.cancel()
        job = null
        tasks.close()
    }
}
