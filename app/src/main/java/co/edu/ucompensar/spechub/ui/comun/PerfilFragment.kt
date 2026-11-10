package co.edu.ucompensar.spechub.ui.comun

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.data.Usuario
import co.edu.ucompensar.spechub.databinding.FragmentPerfilBinding
import co.edu.ucompensar.spechub.ui.LoginActivity
import co.edu.ucompensar.spechub.ui.comprador.DireccionActivity
import co.edu.ucompensar.spechub.util.Biometria
import co.edu.ucompensar.spechub.util.Fmt
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.SelectorFoto
import co.edu.ucompensar.spechub.util.Sesion
import co.edu.ucompensar.spechub.util.confirmar
import co.edu.ucompensar.spechub.util.estadoPill
import co.edu.ucompensar.spechub.util.texto
import co.edu.ucompensar.spechub.util.toast
import co.edu.ucompensar.spechub.util.validar

/** Perfil compartido por los tres roles (el vendedor ve además los datos de su tienda). */
class PerfilFragment : BaseFragment<FragmentPerfilBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentPerfilBinding.inflate(i, c, false)

    private var usuario: Usuario? = null
    private var actualizandoSwitch = false

    private val foto = SelectorFoto(this, { requireContext() }, "perfil") { ruta ->
        usuario?.let { Repositorio.actualizarFoto(it.id, ruta) }
        Imagenes.mostrar(b.ivFoto, ruta, placeholder = R.drawable.ic_person, padding = 20)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val u = Repositorio.usuarioPorId(Sesion.idUsuario) ?: return
        usuario = u

        Imagenes.mostrar(b.ivFoto, u.urlFoto, placeholder = R.drawable.ic_person, padding = 20)
        b.tvNombreCab.text = u.nombre
        b.tvRolCab.estadoPill(u.rol, if (u.rol == "vendedor") "Vendedor verificado" else null)
        b.etNombre.setText(u.nombre)
        b.etCorreo.setText(u.correo)
        b.etTelefono.setText(u.telefono)

        val esVendedor = u.rol == "vendedor"
        b.llTienda.visibility = if (esVendedor) View.VISIBLE else View.GONE
        b.btnDirecciones.visibility = if (u.rol == "comprador") View.VISIBLE else View.GONE
        b.btnEliminarCuenta.visibility = if (u.rol == "administrador") View.GONE else View.VISIBLE
        if (esVendedor) {
            b.etNit.setText(u.nit); b.etCiudad.setText(u.ciudad); b.etMedioCobro.setText(u.medioCobro)
        }

        b.ivFoto.setOnClickListener { foto.dialogo() }
        b.btnFoto.setOnClickListener { foto.dialogo() }
        b.btnDirecciones.setOnClickListener { startActivity(Intent(requireContext(), DireccionActivity::class.java)) }

        actualizandoSwitch = true
        b.swHuella.isChecked = Sesion.huellaUsuario == u.id
        actualizandoSwitch = false
        b.swHuella.setOnCheckedChangeListener { _, activo -> if (!actualizandoSwitch) cambiarHuella(u, activo) }

        b.btnGuardar.setOnClickListener { guardar(u) }
        b.btnCerrarSesion.setOnClickListener { salir() }
        b.btnEliminarCuenta.setOnClickListener {
            requireContext().confirmar(
                "Eliminar cuenta",
                "Tus datos personales se anonimizarán (Ley 1581, derecho de supresión). Esta acción no se puede deshacer.",
                "Eliminar"
            ) {
                Repositorio.eliminarUsuario(u.id, u.id, "Solicitud del titular")
                if (Sesion.huellaUsuario == u.id) Sesion.huellaUsuario = -1
                salir()
            }
        }
    }

    private fun cambiarHuella(u: Usuario, activar: Boolean) {
        val revertir = { actualizandoSwitch = true; b.swHuella.isChecked = !activar; actualizandoSwitch = false }
        if (!activar) { Sesion.huellaUsuario = -1; return }
        if (!Biometria.disponible(requireContext())) {
            requireContext().toast("Este dispositivo no tiene una huella registrada")
            revertir()
            return
        }
        Biometria.pedir(requireActivity(), "Activar ingreso con huella", alFallar = { revertir() }) {
            Sesion.huellaUsuario = u.id
            requireContext().toast("Ingreso con huella activado")
        }
    }

    private fun guardar(u: Usuario) {
        var ok = b.tilNombre.validar(b.tilNombre.texto.length >= 3, "Ingresa tu nombre")
        val tel = b.tilTelefono.texto.filter { it.isDigit() }
        ok = b.tilTelefono.validar(tel.length in 10..12, "Teléfono no válido") && ok
        if (u.rol == "vendedor") ok = b.tilNit.validar(b.tilNit.texto.length >= 6, "Ingresa el NIT") && ok
        if (!ok) return
        Repositorio.actualizarPerfil(
            u.id, b.tilNombre.texto, tel,
            if (u.rol == "vendedor") b.tilNit.texto else u.nit,
            if (u.rol == "vendedor") b.tilCiudad.texto else u.ciudad,
            if (u.rol == "vendedor") b.tilMedioCobro.texto else u.medioCobro
        )
        b.tvNombreCab.text = b.tilNombre.texto
        requireContext().toast("Perfil actualizado")
    }

    private fun salir() {
        Sesion.cerrar()
        startActivity(Intent(requireContext(), LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
}
