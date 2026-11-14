package org.youschool.app

import android.app.Application
import org.youschool.app.core.auth.AuthManager
import org.youschool.app.core.database.YouSchoolDatabase
import org.youschool.app.core.network.EschoolApiClient
import org.youschool.app.data.repository.SchoolRepositoryImpl
import org.youschool.app.domain.repository.SchoolRepository

class YouSchoolApp : Application() {

    lateinit var authManager: AuthManager
        private set

    lateinit var database: YouSchoolDatabase
        private set

    lateinit var apiClient: EschoolApiClient
        private set

    lateinit var repository: SchoolRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        authManager = AuthManager(this)
        database = YouSchoolDatabase.getInstance(this)
        apiClient = EschoolApiClient(authManager)
        repository = SchoolRepositoryImpl(database, apiClient, authManager)
    }

    companion object {
        lateinit var instance: YouSchoolApp
            private set
    }
}
