package com.calculator.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class HistoryItem(val id: Long = 0, val expression: String, val result: String, val timestamp: Long)
