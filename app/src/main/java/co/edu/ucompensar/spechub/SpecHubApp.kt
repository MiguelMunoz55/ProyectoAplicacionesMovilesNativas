package co.edu.ucompensar.spechub

import android.app.Application
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.util.Sesion

class SpecHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Sesion.init(this)
        Repositorio.init(this)
    }
}
