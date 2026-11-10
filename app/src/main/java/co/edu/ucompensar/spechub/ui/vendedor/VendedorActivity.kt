package co.edu.ucompensar.spechub.ui.vendedor

import androidx.fragment.app.Fragment
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.ui.PrincipalActivity
import co.edu.ucompensar.spechub.ui.comun.PerfilFragment

class VendedorActivity : PrincipalActivity() {
    override val menuRes = R.menu.menu_vendedor
    override val inicial = R.id.nav_panel

    override fun fragmentPara(itemId: Int): Pair<Fragment, String> = when (itemId) {
        R.id.nav_panel -> PanelVendedorFragment() to "Panel de vendedor"
        R.id.nav_productos -> ProductosVendedorFragment() to "Mis productos"
        R.id.nav_pedidos -> PedidosVendedorFragment() to "Pedidos recibidos"
        else -> PerfilFragment() to "Mi tienda"
    }
}
