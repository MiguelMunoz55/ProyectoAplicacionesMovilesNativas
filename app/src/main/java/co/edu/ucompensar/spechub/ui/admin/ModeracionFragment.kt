package co.edu.ucompensar.spechub.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.ucompensar.spechub.data.Producto
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentListaBinding
import co.edu.ucompensar.spechub.databinding.ItemProductoVendedorBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.ListaAdapter
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.estadoPill
import co.edu.ucompensar.spechub.util.llenar

/** Moderación de publicaciones (RF-ADP-01). */
class ModeracionFragment : BaseFragment<FragmentListaBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentListaBinding.inflate(i, c, false)

    private val estados = listOf("pendiente", "activo", "rechazado")
    private var estado = "pendiente"

    private val adaptador = ListaAdapter<Producto, ItemProductoVendedorBinding>({ i, p -> ItemProductoVendedorBinding.inflate(i, p, false) }) { v, p ->
        v.tvTitulo.text = p.titulo
        v.tvDetalle.text = "${p.vendedor} · ${Fmt.cop(p.precio)}"
        Imagenes.mostrar(v.ivImagen, p.urlImagen, p.categoria)
        v.tvEstado.estadoPill(p.estado, if (p.estado == "pendiente") "En revisión" else null)
        v.btnEditar.visibility = View.GONE
        v.btnEliminar.visibility = View.GONE
        v.root.setOnClickListener {
            startActivity(Intent(requireContext(), RevisarProductoActivity::class.java).putExtra("id", p.id))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvLista.layoutManager = LinearLayoutManager(requireContext())
        b.rvLista.adapter = adaptador
        b.scrollChips.visibility = View.VISIBLE
        b.tvVacio.text = "No hay productos en esta sección"
        b.chips.llenar(listOf("Pendientes", "Activos", "Rechazados")) { i -> estado = estados[i]; cargar() }
    }

    override fun onResume() { super.onResume(); cargar() }

    private fun cargar() {
        val l = Repositorio.productosPorEstado(estado)
        adaptador.items = l
        b.tvVacio.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
    }
}
