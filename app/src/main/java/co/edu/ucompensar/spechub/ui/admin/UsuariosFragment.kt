package co.edu.ucompensar.spechub.ui.admin

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.data.Repositorio
import co.edu.ucompensar.spechub.data.Usuario
import co.edu.ucompensar.spechub.databinding.FragmentListaBinding
import co.edu.ucompensar.spechub.databinding.ItemUsuarioBinding
import co.edu.ucompensar.spechub.ui.comun.BaseFragment
import co.edu.ucompensar.spechub.ui.comun.ListaAdapter
import co.edu.ucompensar.spechub.util.Imagenes
import co.edu.ucompensar.spechub.util.estadoPill
import co.edu.ucompensar.spechub.util.llenar

/** Listado de usuarios con búsqueda y filtro por rol (RF-ADU-01). */
class UsuariosFragment : BaseFragment<FragmentListaBinding>() {
    override fun inflar(i: LayoutInflater, c: ViewGroup?) = FragmentListaBinding.inflate(i, c, false)

    private val handler = Handler(Looper.getMainLooper())
    private var rol = ""
    private val roles = listOf("", "comprador", "vendedor", "administrador")

    private val adaptador = ListaAdapter<Usuario, ItemUsuarioBinding>({ i, p -> ItemUsuarioBinding.inflate(i, p, false) }) { v, u ->
        v.tvNombre.text = u.nombre
        v.tvCorreo.text = u.correo
        v.tvRol.estadoPill(u.rol)
        Imagenes.mostrar(v.ivAvatar, u.urlFoto, placeholder = R.drawable.ic_person, padding = 8)
        val color = if (u.estado == "activo") R.color.verde else R.color.rojo
        v.vEstado.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color))
        val abrir = { startActivity(Intent(requireContext(), UsuarioFormActivity::class.java).putExtra("id", u.id)) }
        v.root.setOnClickListener { abrir() }
        v.ivEditar.setOnClickListener { abrir() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvLista.layoutManager = LinearLayoutManager(requireContext())
        b.rvLista.adapter = adaptador
        b.tilBuscar.visibility = View.VISIBLE
        b.scrollChips.visibility = View.VISIBLE
        b.fab.visibility = View.VISIBLE
        b.tvVacio.text = "No se encontraron usuarios"
        b.chips.llenar(listOf("Todos", "Comprador", "Vendedor", "Admin")) { i -> rol = roles[i]; cargar() }
        b.etBuscar.doAfterTextChanged { handler.removeCallbacksAndMessages(null); handler.postDelayed({ if (isAdded && view != null) cargar() }, 300) }
        b.fab.setOnClickListener { startActivity(Intent(requireContext(), UsuarioFormActivity::class.java)) }
    }

    override fun onResume() { super.onResume(); cargar() }

    private fun cargar() {
        val l = Repositorio.listarUsuarios(b.etBuscar.text.toString(), rol)
        adaptador.items = l
        b.tvVacio.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { handler.removeCallbacksAndMessages(null); super.onDestroyView() }
}
