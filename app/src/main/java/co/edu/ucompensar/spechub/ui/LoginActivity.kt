package co.edu.ucompensar.spechub.ui

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.data.Usuario
import co.edu.ucompensar.spechub.databinding.ActivityLoginBinding
import co.edu.ucompensar.spechub.util.Biometria
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.abrirPrincipal
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private var fallos = 0
    private var bloqueadoHasta = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnIngresar.setOnClickListener { ingresar() }
        b.tvOlvide.setOnClickListener { startActivity(Intent(this, RecuperarActivity::class.java)) }
        b.btnRegistro.setOnClickListener { startActivity(Intent(this, RegistroActivity::class.java)) }
        b.btnGoogle.setOnClickListener {
            toast("Google Sign-In requiere configurar Firebase Authentication (fase de backend)")
        }
        b.btnHuella.setOnClickListener { ingresarConHuella() }
    }

    private fun ingresar() {
        val ahora = SystemClock.elapsedRealtime()
        if (ahora < bloqueadoHasta) {
            toast("Demasiados intentos. Espera ${(bloqueadoHasta - ahora) / 1000} s")
            return
        }
        val correo = b.tilCorreo.texto
        val clave = b.tilClave.texto
        val ok1 = b.tilCorreo.validar(Patterns.EMAIL_ADDRESS.matcher(correo).matches(), "Correo no válido")
        val ok2 = b.tilClave.validar(clave.isNotEmpty(), "Ingresa tu contraseña")
        if (!ok1 || !ok2) return

        val u = Repositorio.autenticar(correo, clave)
        if (u == null) {
            fallos++
            if (fallos >= 5) { // RNF-SEG-07: bloqueo temporal tras 5 intentos
                bloqueadoHasta = ahora + 30_000
                fallos = 0
                toast("Cuenta bloqueada 30 segundos por intentos fallidos")
            } else {
                b.tilClave.error = "Correo o contraseña incorrectos"
            }
            return
        }
        entrar(u)
    }

    private fun entrar(u: Usuario) {
        if (u.estado != "activo") {
            toast("Tu cuenta está suspendida. Contacta a soporte")
            return
        }
        fallos = 0
        Sesion.iniciar(u)
        abrirPrincipal(u.rol)
    }

    private fun ingresarConHuella() {
        val id = Sesion.huellaUsuario
        if (id < 0) {
            toast("Primero inicia sesión y activa la huella desde tu perfil")
            return
        }
        if (!Biometria.disponible(this)) {
            toast("Este dispositivo no tiene una huella registrada")
            return
        }
        Biometria.pedir(this, "Ingreso con huella") {
            val u = Repositorio.usuarioPorId(id)
            if (u == null) toast("No se encontró la cuenta asociada") else entrar(u)
        }
    }
}
