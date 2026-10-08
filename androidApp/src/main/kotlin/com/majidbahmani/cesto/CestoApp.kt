package com.majidbahmani.cesto

import android.app.Application
import com.majidbahmani.cesto.auth.AndroidGmailAuthorizer
import com.majidbahmani.cesto.di.initKoin
import com.majidbahmani.cesto.di.startBackgroundWork
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory

class CestoApp : Application() {

    /** Created before Koin; MainActivity attaches its consent-screen launcher to it. */
    lateinit var gmailAuthorizer: AndroidGmailAuthorizer
        private set

    override fun onCreate() {
        super.onCreate()
        gmailAuthorizer = AndroidGmailAuthorizer(this)
        initKoin(gmailAuthorizer = gmailAuthorizer) {
            androidLogger()
            androidContext(this@CestoApp)
            // Initializes WorkManager with Koin's WorkerFactory (its automatic setup is off in the manifest).
            workManagerFactory()
        }.startBackgroundWork()
    }
}
