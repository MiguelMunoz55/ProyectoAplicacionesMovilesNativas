package co.edu.ucompensar.spechub.ui.comprador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.ucompensar.spechub.data.Pedido
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentComprasBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.adaptadorPedidos
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

class ComprasFragment : BaseFragment<FragmentComprasBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentComprasBinding.inflate(i, c, false)

    private val adaptador = adaptadorPedidos(
        subtitulo = { Fmt.fechaCorta(it.fecha) + " · " + it.medioPago },
        accion = {
            when (it.estado) {
                "enviado" -> "Seguir envío"
                "entregado" -> if (it.retractoSolicitado) "Retracto solicitado" else "Solicitar retracto"
                else -> "Ver detalle"
            }
        },
        alAccion = { p ->
            when (p.estado) {
                "enviado" -> MaterialAlertDialogBuilder(requireContext()).setTitle("Seguimiento ${p.codigo}")
                    .setMessage("Tu pedido está en camino a:\n${p.direccion}\n\nEstados: pagado → enviado → entregado")
                    .setPositiveButton("Cerrar", null).show()
                "entregado" -> pedirMotivoRetracto(p)
                else -> MaterialAlertDialogBuilder(requireContext()).setTitle("Detalle ${p.codigo}")
                    .setMessage(Repositorio.detallePedido(p.id).joinToString("\n") + "\n\nTotal: ${Fmt.cop(p.total)}\nEntrega: ${p.direccion}")
                    .setPositiveButton("Cerrar", null).show()
            }
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPedidos.adapter = adaptador
        b.tabs.addTab(b.tabs.newTab().setText("Activas"))
        b.tabs.addTab(b.tabs.newTab().setText("Historial"))
        b.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { cargar() }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        val estados = if (b.tabs.selectedTabPosition == 0) listOf("pendiente", "pagado", "enviado") else listOf("entregado", "cancelado")
        val lista = Repositorio.pedidosComprador(Sesion.idUsuario, estados)
        adaptador.items = lista
        b.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    /** RF-PAG-05: el plazo legal de retracto se valida en el servidor; aquí solo se registra la solicitud. */
    private fun pedirMotivoRetracto(p: Pedido) {
        val campo = EditText(requireContext()).apply { hint = "Motivo del retracto" }
        MaterialAlertDialogBuilder(requireContext()).setTitle("Retracto ${p.codigo}")
            .setMessage("Derecho de retracto en ventas a distancia (Ley 1480). Cuéntanos el motivo:")
            .setView(campo)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Enviar solicitud") { _, _ ->
                val motivo = campo.text.toString().trim()
                if (motivo.length < 5) requireContext().toast("Escribe un motivo (mín. 5 caracteres)")
                else { Repositorio.solicitarRetracto(p.id, motivo); requireContext().toast("Solicitud enviada"); cargar() }
            }.show()
    }
}
