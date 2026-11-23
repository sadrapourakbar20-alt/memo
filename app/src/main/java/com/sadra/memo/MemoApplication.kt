package com.sadra.memo

import android.app.Application
import com.sadra.memo.di.AppContainer

class MemoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
