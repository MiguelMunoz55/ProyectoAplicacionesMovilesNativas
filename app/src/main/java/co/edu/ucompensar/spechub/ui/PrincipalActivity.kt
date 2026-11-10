package co.edu.ucompensar.spechub.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.databinding.ActivityPrincipalBinding

/** Contenedor común de los tres roles: barra superior + fragmento + barra de navegación inferior. */
abstract class PrincipalActivity : AppCompatActivity() {
    protected lateinit var b: ActivityPrincipalBinding

    abstract val menuRes: Int
    abstract val inicial: Int
    abstract fun fragmentPara(itemId: Int): Pair<Fragment, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.nav.inflateMenu(menuRes)
        b.nav.setOnItemSelectedListener { mostrar(it.itemId); true }
        if (savedInstanceState == null) b.nav.selectedItemId = inicial
    }

    fun irA(itemId: Int) { b.nav.selectedItemId = itemId }

    private fun mostrar(itemId: Int) {
        val (fragment, titulo) = fragmentPara(itemId)
        b.toolbar.title = titulo
        supportFragmentManager.beginTransaction().replace(R.id.contenedor, fragment).commit()
    }
}
