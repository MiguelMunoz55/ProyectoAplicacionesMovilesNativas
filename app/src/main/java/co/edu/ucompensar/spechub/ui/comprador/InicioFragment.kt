package co.edu.ucompensar.spechub.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentInicioBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.adaptadorProductos
import com.google.android.material.chip.Chip

class InicioFragment : BaseFragment<FragmentInicioBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentInicioBinding.inflate(i, c, false)

    private val adaptador = adaptadorProductos { p ->
        startActivity(Intent(requireContext(), DetalleProductoActivity::class.java).putExtra("id", p.id))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = activity as CompradorActivity
        b.rvDestacados.layoutManager = GridLayoutManager(requireContext(), 2)
        b.rvDestacados.adapter = adaptador

        b.cardBuscar.setOnClickListener { act.irABuscar(0) }
        b.btnVerTodo.setOnClickListener { act.irABuscar(0) }
        b.btnVerOfertas.setOnClickListener {
            val cat = Repositorio.categorias().firstOrNull { it.nombre == "Portátiles" }
            act.irABuscar(cat?.id ?: 0)
        }
        Repositorio.categorias().forEach { cat ->
            val chip = layoutInflater.inflate(R.layout.item_chip, b.chipsCategorias, false) as Chip
            chip.text = cat.nombre
            chip.isCheckable = false
            chip.setOnClickListener { act.irABuscar(cat.id) }
            b.chipsCategorias.addView(chip)
        }
    }

    override fun onResume() {
        super.onResume()
        adaptador.items = Repositorio.buscarProductos().take(8)
    }
}
