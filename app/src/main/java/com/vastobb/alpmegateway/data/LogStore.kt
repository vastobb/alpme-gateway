package com.vastobb.alpmegateway.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LogStore {

    private val _logs = MutableStateFlow<List<RequestLog>>(emptyList())
    val logs: StateFlow<List<RequestLog>> = _logs.asStateFlow()

    fun add(log: RequestLog) {
        _logs.update { current -> (listOf(log) + current).take(MAX_LOGS) }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    private companion object {
        const val MAX_LOGS = 300
    }
}
