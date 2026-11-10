package co.edu.ucompensar.spechub.ui.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentPanelAdminBinding
import co.edu.ucompensar.spechub.databinding.ItemFilaResumenBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.estadoPill

class PanelAdminFragment : BaseFragment<FragmentPanelAdminBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentPanelAdminBinding.inflate(i, c, false)

    override fun onResume() {
        super.onResume()
        val r = Repositorio.resumenAdmin()
        b.tvUsuarios.text = r.usuarios.toString()
        b.tvVendedores.text = r.vendedores.toString()
        b.tvVentasHoy.text = Fmt.cop(r.ventasHoy)
        b.tvPendientes.text = r.pendientes.toString()
        b.grafico.datos = Repositorio.ventasUltimosDias(0, 7)

        b.llPendientes.removeAllViews()
        val pend = Repositorio.productosPorEstado("pendiente")
        b.tvSinPendientes.visibility = if (pend.isEmpty()) View.VISIBLE else View.GONE
        pend.take(5).forEach { p ->
            val v = ItemFilaResumenBinding.inflate(layoutInflater, b.llPendientes, false)
            v.tvTitulo.text = p.titulo
            v.tvSub.text = "${p.vendedor} · producto nuevo"
            Imagenes.mostrar(v.ivIcono, p.urlImagen, p.categoria)
            v.tvPill.estadoPill("pendiente", "Revisar")
            v.root.setOnClickListener {
                startActivity(Intent(requireContext(), RevisarProductoActivity::class.java).putExtra("id", p.id))
            }
            b.llPendientes.addView(v.root)
        }
    }
}
