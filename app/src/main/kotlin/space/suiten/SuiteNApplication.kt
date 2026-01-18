package space.suiten

import android.app.Application
import space.suiten.data.local.SuiteNDatabase
import space.suiten.data.remote.NetworkModule

class SuiteNApplication : Application() {
    val database: SuiteNDatabase by lazy {
        SuiteNDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this)
    }
}
