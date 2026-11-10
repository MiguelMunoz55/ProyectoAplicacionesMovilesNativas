package co.edu.ucompensar.spechub.ui.comprador

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.data.Direccion
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.ActivityDireccionBinding
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.Ubicacion
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar

class DireccionActivity : AppCompatActivity() {
    private lateinit var b: ActivityDireccionBinding
    private var existente: Direccion? = null
    private var lat = 0.0
    private var lon = 0.0

    private val permisos = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) ubicar() else toast("Sin permiso de ubicación: escribe tu dirección manualmente")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDireccionBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }

        existente = Repositorio.direccionPrincipal(Sesion.idUsuario)
        existente?.let {
            b.etCalle.setText(it.calle); b.etCiudad.setText(it.ciudad); b.etReferencia.setText(it.referencia)
            lat = it.latitud; lon = it.longitud
            b.tvUbicacion.text = it.texto
        }
        b.btnUbicacion.setOnClickListener {
            if (Ubicacion.tienePermiso(this)) ubicar() else permisos.launch(Ubicacion.permisos)
        }
        b.btnGuardar.setOnClickListener { guardar() }
    }

    private fun ubicar() {
        b.tvUbicacion.text = "Buscando tu ubicación…"
        Ubicacion.resolver(this) { r ->
            if (r == null) {
                b.tvUbicacion.text = "Ubicación no disponible"
                toast("No se pudo obtener la ubicación. Escribe tu dirección manualmente")
            } else {
                lat = r.latitud; lon = r.longitud
                if (r.calle.isNotBlank()) b.etCalle.setText(r.calle)
                if (r.ciudad.isNotBlank()) b.etCiudad.setText(r.ciudad)
                b.tvUbicacion.text = if (r.calle.isNotBlank()) r.calle else "%.5f, %.5f".format(r.latitud, r.longitud)
            }
        }
    }

    private fun guardar() {
        var ok = b.tilCalle.validar(b.tilCalle.texto.length >= 5, "Ingresa la dirección")
        ok = b.tilCiudad.validar(b.tilCiudad.texto.length >= 3, "Ingresa la ciudad") && ok
        if (!ok) return
        Repositorio.guardarDireccion(
            Direccion(existente?.id ?: 0, Sesion.idUsuario, b.tilCalle.texto, b.tilCiudad.texto, b.tilReferencia.texto, lat, lon)
        )
        toast("Dirección guardada")
        finish()
    }
}
