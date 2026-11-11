package com.budgetkhata.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.budgetkhata.di.AppContainer

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    container: AppContainer,
    key: String? = null,
    crossinline create: (AppContainer, SavedStateHandle) -> VM
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer { create(container, createSavedStateHandle()) }
    }
)
