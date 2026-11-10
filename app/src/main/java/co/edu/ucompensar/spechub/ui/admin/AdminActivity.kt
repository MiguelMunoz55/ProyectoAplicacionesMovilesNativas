package co.edu.ucompensar.spechub.ui.admin

import androidx.fragment.app.Fragment
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.ui.PrincipalActivity
import co.edu.ucompensar.spechub.ui.comun.PerfilFragment

class AdminActivity : PrincipalActivity() {
    override val menuRes = R.menu.menu_admin
    override val inicial = R.id.nav_panel

    override fun fragmentPara(itemId: Int): Pair<Fragment, String> = when (itemId) {
        R.id.nav_panel -> PanelAdminFragment() to "Administración"
        R.id.nav_usuarios -> UsuariosFragment() to "Administrar usuarios"
        R.id.nav_productos -> ModeracionFragment() to "Moderación de productos"
        R.id.nav_pagos -> ReporteVentasFragment() to "Transacciones"
        else -> PerfilFragment() to "Mi perfil"
    }
}
