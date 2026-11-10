package co.edu.ucompensar.spechub.ui.vendedor

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Producto
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.ActivityProductoFormBinding
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.SelectorFoto
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.confirmar
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar

/** Crear / editar producto con cámara (RF-PRD-01, RF-PRD-02, RF-PRD-04). */
class ProductoFormActivity : AppCompatActivity() {
    private lateinit var b: ActivityProductoFormBinding
    private var original: Producto? = null
    private var ruta: String? = null
    private val categorias = Repositorio.categorias()
    private val condiciones = listOf("Nuevo", "Reacondicionado")

    private val foto = SelectorFoto(this, { this }, "producto") { r ->
        ruta = r
        Imagenes.mostrar(b.ivPreview, r, padding = 24)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProductoFormBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }

        b.acCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias.map { it.nombre }))
        b.acCondicion.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, condiciones))
        b.acCondicion.setText(condiciones[0], false)

        val id = intent.getLongExtra("id", 0)
        original = if (id > 0) Repositorio.productoPorId(id) else null
        original?.let { cargar(it) } ?: Imagenes.mostrar(b.ivPreview, null, padding = 24)

        b.btnCamara.setOnClickListener { foto.camara() }
        b.btnGaleria.setOnClickListener { foto.galeria() }
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnEliminar.setOnClickListener {
            val p = original ?: return@setOnClickListener
            confirmar("¿Eliminar producto?", "Se ocultará de la tienda. No se puede eliminar si tiene pedidos activos.", "Eliminar") {
                if (Repositorio.eliminarProducto(p.id)) { toast("Producto eliminado"); finish() }
                else toast("No se puede eliminar: hay pedidos activos con este producto")
            }
        }
    }

    private fun cargar(p: Producto) {
        b.toolbar.title = "Editar producto"
        ruta = p.urlImagen
        Imagenes.mostrar(b.ivPreview, p.urlImagen, p.categoria, padding = 24)
        b.etTitulo.setText(p.titulo); b.etDescripcion.setText(p.descripcion)
        b.acCategoria.setText(p.categoria, false); b.acCondicion.setText(p.condicion, false)
        b.etMarca.setText(p.marca); b.etModelo.setText(p.modelo)
        b.etPrecio.setText(p.precio.toString()); b.etStock.setText(p.stock.toString())
        b.etGarantia.setText(p.mesesGarantia.toString())
        b.etRam.setText(p.ram); b.etAlmacenamiento.setText(p.almacenamiento); b.etBateria.setText(p.bateria)
        b.etImei.setText(p.imei)
        b.btnGuardar.text = "Guardar cambios"
        b.btnEliminar.visibility = View.VISIBLE
        b.tvAviso.text = "Si cambias título, descripción, foto o IMEI el producto vuelve a revisión."
    }

    private fun guardar() {
        val cat = categorias.firstOrNull { it.nombre == b.tilCategoria.texto }
        val precio = b.tilPrecio.texto.toLongOrNull()
        val stock = b.tilStock.texto.toIntOrNull()
        val garantia = b.tilGarantia.texto.toIntOrNull()
        val imei = b.tilImei.texto
        val esCelular = cat?.nombre?.contains("Smartphone", true) == true

        var ok = b.tilTitulo.validar(b.tilTitulo.texto.length >= 5, "Mínimo 5 caracteres")
        ok = b.tilDescripcion.validar(b.tilDescripcion.texto.length >= 10, "Describe el producto (mín. 10 caracteres)") && ok
        ok = b.tilCategoria.validar(cat != null, "Selecciona una categoría") && ok
        ok = b.tilMarca.validar(b.tilMarca.texto.isNotEmpty(), "Ingresa la marca") && ok
        ok = b.tilModelo.validar(b.tilModelo.texto.isNotEmpty(), "Ingresa el modelo") && ok
        ok = b.tilPrecio.validar(precio != null && precio > 0, "Precio en COP mayor a 0") && ok
        ok = b.tilStock.validar(stock != null && stock >= 0, "Stock no válido") && ok
        ok = b.tilGarantia.validar(garantia != null && garantia in 0..60, "Meses de garantía (0-60)") && ok
        ok = b.tilImei.validar(!esCelular || (imei.length == 15 && imei.all { it.isDigit() }), "IMEI de 15 dígitos (obligatorio en smartphones)") && ok
        if (!ok || cat == null || precio == null || stock == null || garantia == null) return

        val o = original
        val cambioRelevante = o == null || o.titulo != b.tilTitulo.texto || o.descripcion != b.tilDescripcion.texto ||
            o.imei != imei || o.urlImagen != ruta
        val p = Producto(
            id = o?.id ?: 0, idVendedor = Sesion.idUsuario, titulo = b.tilTitulo.texto, descripcion = b.tilDescripcion.texto,
            idCategoria = cat.id, categoria = cat.nombre, marca = b.tilMarca.texto, modelo = b.tilModelo.texto,
            precio = precio, stock = stock, condicion = b.tilCondicion.texto.ifBlank { "Nuevo" }, mesesGarantia = garantia,
            ram = b.tilRam.texto, almacenamiento = b.tilAlmacenamiento.texto, bateria = b.tilBateria.texto,
            imei = imei, urlImagen = ruta,
            estado = if (cambioRelevante) "pendiente" else (o?.estado ?: "pendiente")
        )
        Repositorio.guardarProducto(p)
        toast(if (cambioRelevante) "Enviado a revisión del administrador" else "Cambios guardados")
        finish()
    }
}
