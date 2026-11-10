package co.edu.ucompensar.spechub.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.ActivityDetalleBinding
import co.edu.ucompensar.spechub.databinding.ItemSpecBinding
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.estadoPill
import co.edu.ucompensar.spechub.util.toast

class DetalleProductoActivity : AppCompatActivity() {
    private lateinit var b: ActivityDetalleBinding
    private var cantidad = 1
    private var idProducto = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }

        idProducto = intent.getLongExtra("id", 0)
        val p = Repositorio.productoPorId(idProducto)
        if (p == null) { toast("El producto ya no está disponible"); finish(); return }

        Imagenes.mostrar(b.ivImagen, p.urlImagen, p.categoria, padding = 48)
        b.tvTitulo.text = p.titulo
        b.tvPrecio.text = Fmt.cop(p.precio)
        b.tvCondicion.estadoPill("comprador", "${p.condicion} · Garantía ${p.mesesGarantia} meses")
        b.tvDescripcion.text = p.descripcion
        b.tvVendedor.text = "Vendido por ${p.vendedor}"

        fila("Categoría", p.categoria)
        fila("Marca", p.marca)
        fila("Modelo", p.modelo)
        fila("Memoria RAM", p.ram)
        fila("Almacenamiento", p.almacenamiento)
        fila("Batería", p.bateria)
        fila("Condición", p.condicion)
        if (p.imei.isNotBlank()) fila("IMEI / serial", "Registrado y verificado")

        b.tvCantidad.text = cantidad.toString()
        b.btnMenos.setOnClickListener { if (cantidad > 1) { cantidad--; b.tvCantidad.text = cantidad.toString() } }
        b.btnMas.setOnClickListener { if (cantidad < minOf(p.stock, 10)) { cantidad++; b.tvCantidad.text = cantidad.toString() } }

        if (p.stock <= 0) {
            b.btnAgregar.isEnabled = false
            b.btnComprar.isEnabled = false
            b.btnComprar.text = "Agotado"
        }
        b.btnAgregar.setOnClickListener { agregar()?.let { } ?: toast("Agregado al carrito") }
        b.btnComprar.setOnClickListener {
            if (agregar() == null) startActivity(Intent(this, PagoActivity::class.java))
        }
    }

    /** Devuelve null si se agregó correctamente (el error ya se muestra al usuario). */
    private fun agregar(): String? {
        val error = Repositorio.agregarAlCarrito(Sesion.idUsuario, idProducto, cantidad)
        if (error != null) toast(error)
        return error
    }

    private fun fila(etiqueta: String, valor: String) {
        if (valor.isBlank()) return
        val v = ItemSpecBinding.inflate(LayoutInflater.from(this), b.llSpecs, false)
        v.tvEtiqueta.text = etiqueta
        v.tvValor.text = valor
        b.llSpecs.addView(v.root)
    }
}
