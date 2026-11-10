package co.edu.ucompensar.spechub.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.data.Producto
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.ActivityRevisarBinding
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.estadoPill
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar

class RevisarProductoActivity : AppCompatActivity() {
    private lateinit var b: ActivityRevisarBinding
    private lateinit var p: Producto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRevisarBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }

        p = Repositorio.productoPorId(intent.getLongExtra("id", 0)) ?: run { toast("Producto no encontrado"); finish(); return }
        Imagenes.mostrar(b.ivImagen, p.urlImagen, p.categoria, padding = 16)
        b.tvTitulo.text = p.titulo
        b.tvVendedor.text = "Vendedor: ${p.vendedor}"
        b.tvPrecio.text = Fmt.cop(p.precio)
        b.tvEstado.estadoPill(p.estado)
        b.tvFicha.text = listOf(
            "Categoría: ${p.categoria}", "Marca / modelo: ${p.marca} ${p.modelo}", "Condición: ${p.condicion} · ${p.mesesGarantia} meses de garantía",
            "RAM: ${p.ram.ifBlank { "—" }}", "Almacenamiento: ${p.almacenamiento.ifBlank { "—" }}", "IMEI / serial: ${p.imei.ifBlank { "—" }}"
        ).joinToString("\n")
        if (p.estado == "activo") b.btnRechazar.text = "Suspender"

        b.btnAprobar.setOnClickListener {
            if (!(b.cbFotos.isChecked && b.cbSpecs.isChecked && b.cbImei.isChecked)) {
                toast("Completa la lista de verificación antes de aprobar"); return@setOnClickListener
            }
            Repositorio.moderarProducto(p.id, "activo", "", Sesion.idUsuario)
            toast("Producto aprobado")
            finish()
        }
        b.btnRechazar.setOnClickListener {
            val motivo = b.tilMotivo.texto
            if (!b.tilMotivo.validar(motivo.length >= 5, "Motivo obligatorio (mín. 5 caracteres)")) return@setOnClickListener
            val nuevo = if (p.estado == "activo") "suspendido" else "rechazado"
            Repositorio.moderarProducto(p.id, nuevo, motivo, Sesion.idUsuario)
            toast("Producto $nuevo")
            finish()
        }
    }
}
