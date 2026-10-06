package com.majidbahmani.cesto

import android.app.Application
import com.majidbahmani.cesto.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class CestoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@CestoApp)
        }
    }
}
