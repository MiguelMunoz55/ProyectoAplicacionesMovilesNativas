package co.edu.ucompensar.spechub.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.ActivityPagoBinding
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/**
 * Pasarela de pagos (FRONTEND). El cobro se SIMULA: en producción la tarjeta se tokeniza con
 * Wompi (RF-PAG-01) y el estado final lo confirma el servidor por webhook (RF-PAG-03).
 * Ningún dato de tarjeta se guarda en el dispositivo ni en la base de datos.
 */
class PagoActivity : AppCompatActivity() {
    private lateinit var b: ActivityPagoBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPagoBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }

        val items = Repositorio.listarCarrito(Sesion.idUsuario)
        val t = Repositorio.totales(items)
        b.tvPedido.text = "${items.sumOf { it.cantidad }} producto(s) · Envío ${Fmt.cop(t.envio)}"
        b.tvTotal.text = Fmt.cop(t.total)
        b.btnPagar.text = "Pagar ${Fmt.cop(t.total)}"
        if (items.isEmpty()) { toast("Tu carrito está vacío"); finish(); return }

        b.rgMedios.setOnCheckedChangeListener { _, id ->
            b.llTarjeta.visibility = if (id == R.id.rbTarjeta) View.VISIBLE else View.GONE
        }
        b.btnCambiarDireccion.setOnClickListener { startActivity(Intent(this, DireccionActivity::class.java)) }
        b.btnPagar.setOnClickListener { pagar() }
    }

    override fun onResume() {
        super.onResume()
        val dir = Repositorio.direccionPrincipal(Sesion.idUsuario)
        b.tvDireccion.text = dir?.texto ?: "Sin dirección de entrega"
    }

    private fun pagar() {
        val dir = Repositorio.direccionPrincipal(Sesion.idUsuario)
        if (dir == null) {
            toast("Agrega una dirección de entrega para continuar")
            startActivity(Intent(this, DireccionActivity::class.java))
            return
        }
        val medio = when (b.rgMedios.checkedRadioButtonId) {
            R.id.rbPse -> "PSE"
            R.id.rbNequi -> "Nequi"
            R.id.rbBancolombia -> "Bancolombia"
            else -> "Tarjeta"
        }
        if (medio == "Tarjeta" && !validarTarjeta()) return

        b.btnPagar.isEnabled = false
        b.progreso.visibility = View.VISIBLE
        handler.postDelayed({
            val idPedido = Repositorio.crearPedido(Sesion.idUsuario, dir.texto, medio)
            b.progreso.visibility = View.GONE
            if (idPedido > 0) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("¡Pago aprobado!")
                    .setMessage("Tu pedido #SH-${20000 + idPedido} fue registrado. Recibirás el comprobante por correo.")
                    .setCancelable(false)
                    .setPositiveButton("Ver mis compras") { _, _ ->
                        startActivity(
                            Intent(this, CompradorActivity::class.java).putExtra("tab", "compras")
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        finish()
                    }.show()
            } else {
                b.btnPagar.isEnabled = true
                toast("No se pudo completar el pago. Revisa el stock e inténtalo de nuevo")
            }
        }, 1800)
    }

    private fun validarTarjeta(): Boolean {
        val numero = b.tilNumero.texto.filter { it.isDigit() }
        var ok = b.tilNumero.validar(numero.length in 13..19 && luhn(numero), "Número de tarjeta no válido")
        ok = b.tilVence.validar(venceBien(b.tilVence.texto), "Usa el formato MM/AA vigente") && ok
        ok = b.tilCvv.validar(b.tilCvv.texto.length in 3..4 && b.tilCvv.texto.all { it.isDigit() }, "CVV no válido") && ok
        return ok
    }

    private fun luhn(n: String): Boolean {
        var suma = 0
        n.reversed().forEachIndexed { i, c ->
            var d = c - '0'
            if (i % 2 == 1) { d *= 2; if (d > 9) d -= 9 }
            suma += d
        }
        return suma % 10 == 0
    }

    private fun venceBien(s: String): Boolean {
        val m = Regex("^(0[1-9]|1[0-2])/(\\d{2})$").matchEntire(s) ?: return false
        val mes = m.groupValues[1].toInt()
        val anio = 2000 + m.groupValues[2].toInt()
        val hoy = Calendar.getInstance()
        return anio > hoy.get(Calendar.YEAR) || (anio == hoy.get(Calendar.YEAR) && mes >= hoy.get(Calendar.MONTH) + 1)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
