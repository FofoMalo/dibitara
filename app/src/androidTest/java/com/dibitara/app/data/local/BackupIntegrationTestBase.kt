package com.dibitara.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.dibitara.app.data.repository.UserPreferencesRepositoryImpl
import kotlinx.coroutines.*
import org.junit.After
import java.io.File
import java.util.UUID

abstract class BackupIntegrationTestBase : RoomIntegrationTestBase() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected val testPreferences by lazy {
        val context = ApplicationProvider.getApplicationContext<Context>()
        UserPreferencesRepositoryImpl(PreferenceDataStoreFactory.create(scope = scope) {
            File(context.cacheDir, "preferences-${UUID.randomUUID()}.preferences_pb")
        })
    }
    @After fun closePreferences() { scope.cancel() }
}
