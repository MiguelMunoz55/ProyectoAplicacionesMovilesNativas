package co.edu.ucompensar.spechub.ui.vendedor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.ucompensar.spechub.data.Producto
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentListaBinding
import co.edu.ucompensar.spechub.databinding.ItemProductoVendedorBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.ListaAdapter
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.confirmar
import co.edu.ucompensar.spechub.util.estadoPill
import co.edu.ucompensar.spechub.util.llenar
import co.edu.ucompensar.spechub.util.toast

/** CRUD de productos del vendedor: listar, buscar, filtrar, editar y eliminar (crear está en el botón +). */
class ProductosVendedorFragment : BaseFragment<FragmentListaBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentListaBinding.inflate(i, c, false)

    private val handler = Handler(Looper.getMainLooper())
    private var filtro = ""
    private val estados = listOf("", "activo", "pendiente", "rechazado")

    private val adaptador = ListaAdapter<Producto, ItemProductoVendedorBinding>({ i, p -> ItemProductoVendedorBinding.inflate(i, p, false) }) { v, p ->
        v.tvTitulo.text = p.titulo
        v.tvDetalle.text = "${Fmt.cop(p.precio)} · Stock ${p.stock}"
        Imagenes.mostrar(v.ivImagen, p.urlImagen, p.categoria)
        if (p.estado == "activo" && p.stock == 0) v.tvEstado.estadoPill("agotado") else v.tvEstado.estadoPill(p.estado)
        val editar = { startActivity(Intent(requireContext(), ProductoFormActivity::class.java).putExtra("id", p.id)) }
        v.root.setOnClickListener { editar() }
        v.btnEditar.setOnClickListener { editar() }
        v.btnEliminar.setOnClickListener {
            requireContext().confirmar("¿Eliminar producto?", "Se ocultará de la tienda. No se puede eliminar si tiene pedidos activos.", "Eliminar") {
                if (Repositorio.eliminarProducto(p.id)) cargar() else requireContext().toast("No se puede eliminar: hay pedidos activos con este producto")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvLista.layoutManager = LinearLayoutManager(requireContext())
        b.rvLista.adapter = adaptador
        b.tilBuscar.visibility = View.VISIBLE
        b.scrollChips.visibility = View.VISIBLE
        b.fab.visibility = View.VISIBLE
        b.tvVacio.text = "Aún no tienes productos. Toca + para publicar el primero"
        b.chips.llenar(listOf("Todos", "Activos", "Pendientes", "Rechazados")) { i -> filtro = estados[i]; cargar() }
        b.etBuscar.doAfterTextChanged { handler.removeCallbacksAndMessages(null); handler.postDelayed({ if (_attached()) cargar() }, 300) }
        b.fab.setOnClickListener { startActivity(Intent(requireContext(), ProductoFormActivity::class.java)) }
    }

    private fun _attached() = view != null && isAdded

    override fun onResume() { super.onResume(); cargar() }

    private fun cargar() {
        val l = Repositorio.productosVendedor(Sesion.idUsuario, filtro, b.etBuscar.text.toString())
        adaptador.items = l
        b.tvVacio.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { handler.removeCallbacksAndMessages(null); super.onDestroyView() }
}
