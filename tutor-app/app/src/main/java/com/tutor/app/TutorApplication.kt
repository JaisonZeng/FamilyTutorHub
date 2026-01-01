package com.tutor.app

import android.app.Application

class TutorApplication : Application() {
    
    companion object {
        lateinit var instance: TutorApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
