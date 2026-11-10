package co.edu.ucompensar.spechub.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.abrirPrincipal

class SplashActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed({ continuar() }, 1500)
    }

    private fun continuar() {
        val u = if (Sesion.idUsuario > 0) Repositorio.usuarioPorId(Sesion.idUsuario) else null
        if (u != null && u.estado == "activo") {
            abrirPrincipal(u.rol)
        } else {
            Sesion.cerrar()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
