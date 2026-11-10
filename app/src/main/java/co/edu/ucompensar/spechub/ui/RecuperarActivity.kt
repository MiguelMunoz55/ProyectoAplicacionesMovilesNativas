package co.edu.ucompensar.spechub.ui

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.databinding.ActivityRecuperarBinding
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.validar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RecuperarActivity : AppCompatActivity() {
    private lateinit var b: ActivityRecuperarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRecuperarBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnEnviar.setOnClickListener {
            val correo = b.tilCorreo.texto
            if (!b.tilCorreo.validar(Patterns.EMAIL_ADDRESS.matcher(correo).matches(), "Correo no válido")) return@setOnClickListener
            // RF-AUT-04: respuesta genérica, no revela si el correo existe.
            MaterialAlertDialogBuilder(this)
                .setTitle("Revisa tu correo")
                .setMessage("Si $correo está registrado, recibirás un enlace para restablecer tu contraseña. El enlace vence en 30 minutos y se usa una sola vez.")
                .setPositiveButton("Entendido") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }
}
