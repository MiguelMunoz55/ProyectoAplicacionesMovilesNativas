package co.edu.ucompensar.spechub.util

import android.content.Context
import android.content.SharedPreferences
import co.edu.ucompensar.spechub.data.Usuario

/** Sesión local. En producción aquí viviría el token cifrado con Android Keystore (RF-AUT-05). */
object Sesion {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        sp = c.applicationContext.getSharedPreferences("sesion", Context.MODE_PRIVATE)
    }

    var idUsuario: Long
        get() = sp.getLong("id", -1L)
        set(v) { sp.edit().putLong("id", v).apply() }

    var rol: String
        get() = sp.getString("rol", "") ?: ""
        set(v) { sp.edit().putString("rol", v).apply() }

    /** Id del usuario que activó el ingreso con huella en este dispositivo (-1 = ninguno). */
    var huellaUsuario: Long
        get() = sp.getLong("huella", -1L)
        set(v) { sp.edit().putLong("huella", v).apply() }

    fun iniciar(u: Usuario) {
        idUsuario = u.id
        rol = u.rol
    }

    fun cerrar() {
        sp.edit().remove("id").remove("rol").apply()
    }
}
