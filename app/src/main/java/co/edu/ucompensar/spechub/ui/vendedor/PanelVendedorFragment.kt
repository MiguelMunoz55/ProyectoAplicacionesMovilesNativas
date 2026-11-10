package co.edu.ucompensar.spechub.ui.vendedor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.FragmentPanelVendedorBinding
import co.edu.ucompensar.spechub.databinding.ItemFilaResumenBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.estadoPill

class PanelVendedorFragment : BaseFragment<FragmentPanelVendedorBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentPanelVendedorBinding.inflate(i, c, false)

    override fun onResume() {
        super.onResume()
        val id = Sesion.idUsuario
        val u = Repositorio.usuarioPorId(id)
        b.tvSaludo.text = "Hola, ${u?.nombre ?: ""}"
        b.tvVentasMes.text = Fmt.cop(Repositorio.ventasMesVendedor(id))
        b.tvPedidosNuevos.text = Repositorio.pedidosNuevosVendedor(id).toString()
        b.grafico.datos = Repositorio.ventasUltimosDias(id, 7)

        b.llStockBajo.removeAllViews()
        val bajos = Repositorio.stockBajo(id)
        b.tvSinStockBajo.visibility = if (bajos.isEmpty()) View.VISIBLE else View.GONE
        bajos.forEach { p ->
            val v = ItemFilaResumenBinding.inflate(layoutInflater, b.llStockBajo, false)
            v.tvTitulo.text = p.titulo
            v.tvSub.text = "Stock: ${p.stock} unidades"
            Imagenes.mostrar(v.ivIcono, p.urlImagen, p.categoria)
            v.tvPill.estadoPill("pendiente", "Reponer")
            b.llStockBajo.addView(v.root)
        }
    }
}
