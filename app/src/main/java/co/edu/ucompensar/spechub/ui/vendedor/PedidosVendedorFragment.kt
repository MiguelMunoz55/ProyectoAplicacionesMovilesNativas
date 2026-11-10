package co.edu.ucompensar.spechub.ui.vendedor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentListaBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.adaptadorPedidos
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.confirmar
import co.edu.ucompensar.spechub.util.llenar

/** Historial de pedidos recibidos (Nuevos / Enviados / Cerrados). */
class PedidosVendedorFragment : BaseFragment<FragmentListaBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentListaBinding.inflate(i, c, false)

    private var pestana = 0

    private val adaptador = adaptadorPedidos(
        subtitulo = { "${it.comprador} · ${Fmt.fechaCorta(it.fecha)}" },
        accion = { when (it.estado) { "pagado", "pendiente" -> "Marcar como enviado"; "enviado" -> "Marcar como entregado"; else -> null } },
        alAccion = { p ->
            val siguiente = if (p.estado == "enviado") "entregado" else "enviado"
            requireContext().confirmar("Pedido ${p.codigo}", "¿Marcar como $siguiente?") {
                Repositorio.cambiarEstadoPedido(p.id, siguiente)
                cargar()
            }
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvLista.layoutManager = LinearLayoutManager(requireContext())
        b.rvLista.adapter = adaptador
        b.scrollChips.visibility = View.VISIBLE
        b.tvVacio.text = "No hay pedidos en esta sección"
        b.chips.llenar(listOf("Nuevos", "Enviados", "Cerrados")) { i -> pestana = i; cargar() }
    }

    override fun onResume() { super.onResume(); cargar() }

    private fun cargar() {
        val estados = when (pestana) { 0 -> listOf("pendiente", "pagado"); 1 -> listOf("enviado"); else -> listOf("entregado", "cancelado") }
        val l = Repositorio.pedidosVendedor(Sesion.idUsuario, estados)
        adaptador.items = l
        b.tvVacio.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
    }
}
