package co.edu.ucompensar.spechub.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import co.edu.ucompensar.spechub.data.Categoria
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentCatalogoBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.adaptadorProductos
import co.edu.ucompensar.spechub.util.llenar

class CatalogoFragment : BaseFragment<FragmentCatalogoBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentCatalogoBinding.inflate(i, c, false)

    private var idCategoria = 0L
    private var condicion = ""
    private val handler = Handler(Looper.getMainLooper())
    private var categorias: List<Categoria> = emptyList()

    private val adaptador = adaptadorProductos { p ->
        startActivity(Intent(requireContext(), DetalleProductoActivity::class.java).putExtra("id", p.id))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = activity as CompradorActivity
        idCategoria = act.filtroCategoria
        b.etBuscar.setText(act.filtroTexto)
        act.filtroCategoria = 0
        act.filtroTexto = ""

        categorias = Repositorio.categorias()
        b.rvProductos.layoutManager = GridLayoutManager(requireContext(), 2)
        b.rvProductos.adapter = adaptador

        b.chipsCategoria.llenar(
            listOf("Todas") + categorias.map { it.nombre },
            categorias.indexOfFirst { it.id == idCategoria } + 1
        ) { i ->
            idCategoria = if (i == 0) 0 else categorias[i - 1].id
            buscar()
        }
        val condiciones = listOf("Nuevo", "Reacondicionado")
        b.chipsCondicion.llenar(listOf("Todas") + condiciones) { i ->
            condicion = if (i == 0) "" else condiciones[i - 1]
            buscar()
        }
        // RF-INI-02: retardo de 300 ms al escribir
        b.etBuscar.doAfterTextChanged {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ if (view.isAttachedToWindow) buscar() }, 300)
        }
        buscar()
    }

    private fun buscar() {
        val lista = Repositorio.buscarProductos(b.etBuscar.text.toString(), idCategoria, condicion)
        adaptador.items = lista
        b.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}
