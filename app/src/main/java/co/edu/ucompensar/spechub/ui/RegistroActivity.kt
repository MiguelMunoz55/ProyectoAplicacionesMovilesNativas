package co.edu.ucompensar.spechub.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.databinding.ActivityRegistroBinding
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.SelectorFoto
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.Ubicacion
import co.edu.ucompensar.spechub.util.abrirPrincipal
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar

class RegistroActivity : AppCompatActivity() {
    private lateinit var b: ActivityRegistroBinding
    private var fotoRuta: String? = null

    private val foto = SelectorFoto(this, { this }, "perfil") { ruta ->
        fotoRuta = ruta
        Imagenes.mostrar(b.ivFoto, ruta, placeholder = R.drawable.ic_person, padding = 20)
    }

    private val permisosUbicacion = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) ubicar() else toast("Sin permiso de ubicación: escribe tu ciudad manualmente")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }
        Imagenes.mostrar(b.ivFoto, null, placeholder = R.drawable.ic_person, padding = 20)
        b.btnFoto.setOnClickListener { foto.camara() }
        b.btnGaleria.setOnClickListener { foto.galeria() }

        b.toggleRol.check(R.id.btnRolComprador)
        b.toggleRol.addOnButtonCheckedListener { _, id, marcado ->
            if (marcado) b.tilNit.visibility = if (id == R.id.btnRolVendedor) View.VISIBLE else View.GONE
        }
        b.btnGps.setOnClickListener {
            if (Ubicacion.tienePermiso(this)) ubicar() else permisosUbicacion.launch(Ubicacion.permisos)
        }
        b.btnRegistrar.setOnClickListener { registrar() }
    }

    private fun ubicar() {
        Ubicacion.resolver(this) { r ->
            if (r == null || r.ciudad.isBlank()) toast("No se pudo obtener la ubicación. Escribe tu ciudad manualmente")
            else b.etCiudad.setText(r.ciudad)
        }
    }

    private fun registrar() {
        val esVendedor = b.toggleRol.checkedButtonId == R.id.btnRolVendedor
        val clave = b.tilClave.texto
        val tel = b.tilTelefono.texto.filter { it.isDigit() }
        val correo = b.tilCorreo.texto

        var ok = b.tilNombre.validar(b.tilNombre.texto.length >= 3, "Ingresa tu nombre completo")
        ok = b.tilCorreo.validar(Patterns.EMAIL_ADDRESS.matcher(correo).matches(), "Correo no válido") && ok
        ok = b.tilTelefono.validar(tel.length in 10..12, "Teléfono no válido") && ok
        ok = b.tilClave.validar(
            clave.length >= 8 && clave.any { it.isUpperCase() } && clave.any { it.isDigit() },
            "Mínimo 8 caracteres, una mayúscula y un número"
        ) && ok
        if (esVendedor) ok = b.tilNit.validar(b.tilNit.texto.length >= 6, "Ingresa tu cédula o NIT") && ok
        if (!ok) return
        if (!b.cbMayor.isChecked) { toast("Debes confirmar que eres mayor de edad"); return }
        if (!b.cbDatos.isChecked) { toast("Debes aceptar el tratamiento de datos (Ley 1581)"); return }

        val id = Repositorio.crearUsuario(
            b.tilNombre.texto, correo, tel, clave, if (esVendedor) "vendedor" else "comprador",
            nit = b.tilNit.texto, ciudad = b.tilCiudad.texto, urlFoto = fotoRuta
        )
        if (id < 0) { b.tilCorreo.error = "Este correo ya está registrado"; return }

        val u = Repositorio.usuarioPorId(id) ?: return
        toast("Cuenta creada. Te enviaremos un correo de verificación")
        Sesion.iniciar(u)
        abrirPrincipal(u.rol)
    }
}
