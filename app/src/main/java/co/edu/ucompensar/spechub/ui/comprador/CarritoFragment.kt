package co.edu.ucompensar.spechub.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.ucompensar.spechub.data.ItemCarrito
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentCarritoBinding
import co.edu.ucompensar.spechub.databinding.ItemCarritoBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.ListaAdapter
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.toast

class CarritoFragment : BaseFragment<FragmentCarritoBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentCarritoBinding.inflate(i, c, false)

    private val adaptador = ListaAdapter<ItemCarrito, ItemCarritoBinding>({ i, p -> ItemCarritoBinding.inflate(i, p, false) }) { v, item ->
        v.tvTitulo.text = item.titulo
        v.tvPrecio.text = Fmt.cop(item.precio)
        v.tvCantidad.text = item.cantidad.toString()
        Imagenes.mostrar(v.ivImagen, item.urlImagen, item.categoria)
        v.btnMenos.setOnClickListener {
            if (item.cantidad > 1) { Repositorio.cambiarCantidad(item.id, item.cantidad - 1); cargar() }
        }
        v.btnMas.setOnClickListener {
            val error = Repositorio.cambiarCantidad(item.id, item.cantidad + 1)
            if (error != null) requireContext().toast(error)
            cargar()
        }
        v.btnEliminar.setOnClickListener { Repositorio.quitarItem(item.id); cargar() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvItems.layoutManager = LinearLayoutManager(requireContext())
        b.rvItems.adapter = adaptador
        b.cardDireccion.setOnClickListener { startActivity(Intent(requireContext(), DireccionActivity::class.java)) }
        b.btnPagar.setOnClickListener {
            startActivity(Intent(requireContext(), PagoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        val items = Repositorio.listarCarrito(Sesion.idUsuario)
        adaptador.items = items
        val t = Repositorio.totales(items)
        b.tvSubtotal.text = Fmt.cop(t.subtotal)
        b.tvEnvio.text = Fmt.cop(t.envio)
        b.tvIva.text = Fmt.cop(t.iva)
        b.tvTotal.text = Fmt.cop(t.total)
        val vacio = items.isEmpty()
        b.tvVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        b.llResumen.visibility = if (vacio) View.GONE else View.VISIBLE
        b.btnPagar.isEnabled = !vacio
        val dir = Repositorio.direccionPrincipal(Sesion.idUsuario)
        b.tvDireccion.text = if (dir == null) "Agrega una dirección de entrega" else "Entrega: ${dir.texto}"
        (activity as? CompradorActivity)?.actualizarBadge()
    }
}
