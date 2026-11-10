package co.edu.ucompensar.spechub.ui.comprador

import android.content.Intent
import androidx.fragment.app.Fragment
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.ui.PrincipalActivity
import co.edu.ucompensar.spechub.ui.comun.PerfilFragment
import co.edu.ucompensar.spechub.util.Sesion

class CompradorActivity : PrincipalActivity() {
    override val menuRes = R.menu.menu_comprador
    override val inicial = R.id.nav_inicio

    /** Filtros pendientes que Inicio entrega a la pestaña Buscar. */
    var filtroCategoria = 0L
    var filtroTexto = ""

    override fun fragmentPara(itemId: Int): Pair<Fragment, String> = when (itemId) {
        R.id.nav_inicio -> InicioFragment() to "SpecHub"
        R.id.nav_buscar -> CatalogoFragment() to "Catálogo"
        R.id.nav_carrito -> CarritoFragment() to "Mi carrito"
        R.id.nav_compras -> ComprasFragment() to "Mis compras"
        else -> PerfilFragment() to "Mi perfil"
    }

    fun irABuscar(idCategoria: Long, texto: String = "") {
        filtroCategoria = idCategoria
        filtroTexto = texto
        irA(R.id.nav_buscar)
    }

    fun actualizarBadge() {
        val n = Repositorio.contarCarrito(Sesion.idUsuario)
        if (n > 0) {
            val badge = b.nav.getOrCreateBadge(R.id.nav_carrito)
            badge.isVisible = true
            badge.number = n
        } else {
            b.nav.removeBadge(R.id.nav_carrito)
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarBadge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra("tab") == "compras") irA(R.id.nav_compras)
    }
}
