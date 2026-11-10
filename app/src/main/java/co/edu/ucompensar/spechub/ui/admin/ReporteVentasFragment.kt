package co.edu.ucompensar.spechub.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.ucompensar.spechub.data.ReporteVentas
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentReporteBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.adaptadorPedidos
import co.edu.ucompensar.spechub.util.Fmt

/** Reporte de ventas / transacciones con comisión de la plataforma (5 %). */
class ReporteVentasFragment : BaseFragment<FragmentReporteBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentReporteBinding.inflate(i, c, false)

    private var reporte: ReporteVentas? = null
    private val adaptador = adaptadorPedidos(
        subtitulo = { "${it.comprador} · ${it.medioPago}" },
        accion = { null },
        alAccion = { }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPedidos.adapter = adaptador
        b.btnExportar.setOnClickListener { exportar() }
    }

    override fun onResume() {
        super.onResume()
        val r = Repositorio.reporteVentas()
        reporte = r
        b.tvComision.text = Fmt.cop(r.comision)
        b.tvResumen.text = "${r.transacciones} transacciones · ${r.aprobadasPct}% aprobadas"
        adaptador.items = r.filas
    }

    private fun exportar() {
        val r = reporte ?: return
        val csv = StringBuilder("codigo,comprador,total_cop,estado,medio_pago,fecha\n")
        r.filas.forEach { csv.append("${it.codigo},\"${it.comprador}\",${it.total},${it.estado},${it.medioPago},${it.fecha}\n") }
        val i = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Reporte de ventas SpecHub").putExtra(Intent.EXTRA_TEXT, csv.toString())
        startActivity(Intent.createChooser(i, "Exportar reporte (CSV)"))
    }
}
