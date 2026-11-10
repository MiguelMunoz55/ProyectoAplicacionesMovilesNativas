package co.edu.ucompensar.spechub.ui.comun

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Pedido
import co.edu.ucompensar.spechub.data.Producto
import co.edu.ucompensar.spechub.databinding.ItemPedidoBinding
import co.edu.ucompensar.spechub.databinding.ItemProductoGridBinding
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.estadoPill

/** Adaptador genérico: se le pasa cómo inflar el ViewBinding y cómo enlazar cada fila. */
class ListaAdapter<T, B : ViewBinding>(
    private val inflar: (LayoutInflater, ViewGroup) -> B,
    private val enlazar: (B, T) -> Unit
) : RecyclerView.Adapter<VistaHolder<B>>() {

    var items: List<T> = emptyList()
        set(v) { field = v; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VistaHolder(inflar(LayoutInflater.from(parent.context), parent))

    override fun onBindViewHolder(holder: VistaHolder<B>, position: Int) = enlazar(holder.b, items[position])
    override fun getItemCount() = items.size
}

class VistaHolder<B : ViewBinding>(val b: B) : RecyclerView.ViewHolder(b.root)

/** Fragment con ViewBinding que libera la referencia al destruir la vista. */
abstract class BaseFragment<B : ViewBinding> : Fragment() {
    private var _b: B? = null
    protected val b: B get() = _b!!
    protected abstract fun inflar(i: LayoutInflater, c: ViewGroup?): B

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: android.os.Bundle?): View {
        _b = inflar(i, c)
        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

fun adaptadorProductos(alClic: (Producto) -> Unit) =
    ListaAdapter<Producto, ItemProductoGridBinding>({ i, p -> ItemProductoGridBinding.inflate(i, p, false) }) { v, p ->
        v.tvTitulo.text = p.titulo
        v.tvPrecio.text = Fmt.cop(p.precio)
        v.tvVendedor.text = if (p.stock <= 0) "Agotado" else p.vendedor
        Imagenes.mostrar(v.ivImagen, p.urlImagen, p.categoria)
        v.root.setOnClickListener { alClic(p) }
    }

/**
 * Adaptador de pedidos reutilizado por Compras (comprador), Pedidos recibidos (vendedor)
 * y Transacciones (administrador).
 */
fun adaptadorPedidos(
    subtitulo: (Pedido) -> String,
    accion: (Pedido) -> String?,
    alAccion: (Pedido) -> Unit
) = ListaAdapter<Pedido, ItemPedidoBinding>({ i, p -> ItemPedidoBinding.inflate(i, p, false) }) { v, p ->
    v.tvCodigo.text = p.codigo
    v.tvEstado.estadoPill(p.estado)
    v.tvFecha.text = subtitulo(p)
    v.tvResumen.text = p.resumen
    v.tvTotal.text = Fmt.cop(p.total)
    val txt = accion(p)
    v.btnAccion.visibility = if (txt == null) View.GONE else View.VISIBLE
    v.btnAccion.text = txt
    v.btnAccion.isEnabled = txt != "Retracto solicitado"
    v.btnAccion.setOnClickListener { alAccion(p) }
}

/** Gráfico de barras simple (ventas de los últimos 7 días). */
class GraficoBarrasView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    var datos: List<Pair<String, Long>> = emptyList()
        set(v) { field = v; invalidate() }

    private val pBarra = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(c, R.color.texto_secundario)
        textSize = 11f * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (datos.isEmpty()) return
        val max = datos.maxOf { it.second }.coerceAtLeast(1L)
        val n = datos.size
        val w = width.toFloat()
        val h = height.toFloat()
        val franja = pTexto.textSize * 1.8f
        val area = h - franja
        val paso = w / n
        val ancho = paso * 0.55f
        val minimo = 4f * resources.displayMetrics.density
        datos.forEachIndexed { i, par ->
            val alto = maxOf(area * par.second / max, minimo)
            val x = paso * i + (paso - ancho) / 2
            pBarra.color = ContextCompat.getColor(context, if (i == n - 1) R.color.azul_spechub else R.color.azul_barra)
            canvas.drawRoundRect(RectF(x, area - alto, x + ancho, area), 8f, 8f, pBarra)
            canvas.drawText(par.first, paso * i + paso / 2, h - franja * 0.25f, pTexto)
        }
    }
}
