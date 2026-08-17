package com.docuvio.app

import android.app.Application
import com.docuvio.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DocuvioApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        appContainer = AppContainer(
            context = applicationContext,
            onUnauthorized = {
                // Handle 401 - force logout
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(
                        applicationContext,
                        "Session expired. Please login again.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    appContainer.authRepository.logout()
                }
            }
        )

    }
}