package com.budgetkhata.core

sealed interface SaveResult {
    data class Success(val id: Long) : SaveResult
    data class Error(val message: String) : SaveResult
}
