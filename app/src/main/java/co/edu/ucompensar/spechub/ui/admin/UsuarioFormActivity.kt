package co.edu.ucompensar.spechub.ui.admin

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.data.Usuario
import co.edu.ucompensar.spechub.databinding.ActivityUsuarioFormBinding
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.confirmar
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Crear / editar / suspender / eliminar usuario. Las acciones críticas exigen motivo y quedan en auditoría. */
class UsuarioFormActivity : AppCompatActivity() {
    private lateinit var b: ActivityUsuarioFormBinding
    private var usuario: Usuario? = null
    private val nombresRol = listOf("Comprador", "Vendedor", "Administrador")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUsuarioFormBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { finish() }
        b.acRol.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresRol))
        b.acRol.setText(nombresRol[0], false)
        Imagenes.mostrar(b.ivAvatar, null, placeholder = R.drawable.ic_person, padding = 20)

        val id = intent.getLongExtra("id", 0)
        usuario = if (id > 0) Repositorio.usuarioPorId(id) else null
        val u = usuario
        if (u != null) {
            b.toolbar.title = "Editar usuario"
            b.tvNombreCab.text = u.nombre
            b.etNombre.setText(u.nombre); b.etCorreo.setText(u.correo); b.etTelefono.setText(u.telefono)
            b.etCorreo.isEnabled = false
            b.acRol.setText(u.rol.replaceFirstChar { it.uppercase() }, false)
            Imagenes.mostrar(b.ivAvatar, u.urlFoto, placeholder = R.drawable.ic_person, padding = 20)
            b.btnSuspender.text = if (u.estado == "activo") "Suspender" else "Reactivar"
            b.btnSuspender.visibility = View.VISIBLE
            b.btnEliminar.visibility = View.VISIBLE
            b.btnGuardar.text = "Guardar"
        } else {
            b.toolbar.title = "Nuevo usuario"
            b.tilMotivo.visibility = View.GONE
            b.tvNombreCab.text = "Nuevo usuario"
        }
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnSuspender.setOnClickListener { suspender() }
        b.btnEliminar.setOnClickListener { eliminar() }
    }

    private fun esUnoMismo() = usuario?.id == Sesion.idUsuario

    private fun motivoValido(): String? {
        val m = b.tilMotivo.texto
        return if (b.tilMotivo.validar(m.length >= 5, "Motivo obligatorio (mín. 5 caracteres)")) m else null
    }

    private fun rolSeleccionado() = b.tilRol.texto.lowercase().let { if (it == "administrador" || it == "vendedor") it else "comprador" }

    private fun guardar() {
        val tel = b.tilTelefono.texto.filter { it.isDigit() }
        var ok = b.tilNombre.validar(b.tilNombre.texto.length >= 3, "Ingresa el nombre")
        ok = b.tilTelefono.validar(tel.length in 10..12, "Teléfono no válido") && ok
        val u = usuario
        if (u == null) {
            ok = b.tilCorreo.validar(Patterns.EMAIL_ADDRESS.matcher(b.tilCorreo.texto).matches(), "Correo no válido") && ok
            if (!ok) return
            val temporal = claveTemporal()
            val id = Repositorio.crearUsuario(b.tilNombre.texto, b.tilCorreo.texto, tel, temporal, rolSeleccionado())
            if (id < 0) { b.tilCorreo.error = "Este correo ya está registrado"; return }
            Repositorio.auditar(Sesion.idUsuario, "crear_usuario", "usuario:$id", "", rolSeleccionado())
            MaterialAlertDialogBuilder(this).setTitle("Usuario creado")
                .setMessage("Contraseña temporal:\n\n$temporal\n\nEl usuario deberá cambiarla en su primer ingreso.")
                .setCancelable(false).setPositiveButton("Entendido") { _, _ -> finish() }.show()
            return
        }
        if (!ok) return
        if (esUnoMismo() && rolSeleccionado() != u.rol) { toast("Un administrador no puede cambiar su propio rol"); return }
        val motivo = motivoValido() ?: return
        Repositorio.actualizarUsuarioAdmin(u.id, b.tilNombre.texto, tel, rolSeleccionado(), Sesion.idUsuario, motivo)
        toast("Usuario actualizado")
        finish()
    }

    private fun suspender() {
        val u = usuario ?: return
        if (esUnoMismo()) { toast("No puedes suspender tu propia cuenta"); return }
        val motivo = motivoValido() ?: return
        val nuevo = if (u.estado == "activo") "suspendido" else "activo"
        confirmar("Confirmar", "¿Cambiar el estado de ${u.nombre} a $nuevo?") {
            Repositorio.cambiarEstadoUsuario(u.id, nuevo, Sesion.idUsuario, motivo)
            toast("Estado actualizado")
            finish()
        }
    }

    private fun eliminar() {
        val u = usuario ?: return
        if (esUnoMismo()) { toast("No puedes eliminar tu propia cuenta desde aquí"); return }
        val motivo = motivoValido() ?: return
        confirmar("Eliminar cuenta (borrado lógico)", "Se anonimizarán los datos personales de ${u.nombre}. No se puede deshacer.", "Eliminar") {
            Repositorio.eliminarUsuario(u.id, Sesion.idUsuario, motivo)
            toast("Cuenta eliminada")
            finish()
        }
    }

    private fun claveTemporal(): String {
        val letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val base = (1..6).map { letras.random() }.joinToString("")
        return "${base}A${(10..99).random()}"
    }
}
