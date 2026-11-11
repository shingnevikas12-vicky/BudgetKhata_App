package com.budgetkhata

import android.app.Application
import com.budgetkhata.di.AppContainer

class BudgetKhataApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
