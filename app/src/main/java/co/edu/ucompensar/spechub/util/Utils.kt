package co.edu.ucompensar.spechub.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import co.edu.ucompensar.spechub.R
import co.edu.ucompensar.spechub.ui.admin.AdminActivity
import co.edu.ucompensar.spechub.ui.comprador.CompradorActivity
import co.edu.ucompensar.spechub.ui.vendedor.VendedorActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------ formato
object Fmt {
    private val locale = Locale("es", "CO")

    fun cop(v: Long): String = "\$" + NumberFormat.getNumberInstance(locale).format(v)

    fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(("spechub::$s").toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun ahora(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    fun fechaCorta(f: String): String = try {
        val d = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(f)
        SimpleDateFormat("dd MMM yyyy", locale).format(d!!)
    } catch (e: Exception) { f }

    fun estadoTexto(e: String): String = when (e) {
        "enviado" -> "En camino"
        "administrador" -> "Admin"
        else -> e.replaceFirstChar { it.uppercase() }
    }
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
fun Context.toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

fun Context.confirmar(titulo: String, mensaje: String, ok: String = "Aceptar", alAceptar: () -> Unit) {
    MaterialAlertDialogBuilder(this).setTitle(titulo).setMessage(mensaje)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(ok) { _, _ -> alAceptar() }.show()
}

fun Context.abrirPrincipal(rol: String) {
    val destino = when (rol) {
        "administrador" -> AdminActivity::class.java
        "vendedor" -> VendedorActivity::class.java
        else -> CompradorActivity::class.java
    }
    startActivity(Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
}

// ------------------------------------------------------------------ componentes
fun TextInputLayout.validar(ok: Boolean, mensaje: String): Boolean {
    error = if (ok) null else mensaje
    return ok
}

val TextInputLayout.texto: String get() = editText?.text?.toString()?.trim() ?: ""

fun TextView.estadoPill(estado: String, textoPersonalizado: String? = null) {
    val (fg, bg) = when (estado.lowercase()) {
        "activo", "entregado" -> R.color.verde_texto to R.color.verde_claro
        "pendiente" -> R.color.ambar_texto to R.color.ambar_claro
        "pagado", "enviado", "comprador" -> R.color.azul_spechub to R.color.azul_claro
        "vendedor" -> R.color.cian_oscuro to R.color.cian_claro
        "rechazado", "suspendido", "cancelado", "agotado" -> R.color.rojo_texto to R.color.rojo_claro
        else -> R.color.texto_secundario to R.color.gris_claro
    }
    text = textoPersonalizado ?: Fmt.estadoTexto(estado)
    setTextColor(ContextCompat.getColor(context, fg))
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, bg))
}

/** Llena un ChipGroup de selección única; [alCambiar] recibe el índice seleccionado. */
fun ChipGroup.llenar(opciones: List<String>, seleccion: Int = 0, alCambiar: (Int) -> Unit) {
    removeAllViews()
    isSingleSelection = true
    isSelectionRequired = true
    val inf = LayoutInflater.from(context)
    opciones.forEachIndexed { i, t ->
        val chip = inf.inflate(R.layout.item_chip, this, false) as Chip
        chip.id = View.generateViewId()
        chip.text = t
        chip.tag = i
        chip.isChecked = i == seleccion
        addView(chip)
    }
    setOnCheckedStateChangeListener { g, ids ->
        if (ids.isNotEmpty()) alCambiar(g.findViewById<Chip>(ids[0]).tag as Int)
    }
}

// ------------------------------------------------------------------ imágenes
object Imagenes {
    fun iconoCategoria(cat: String): Int = when {
        cat.contains("Smartphone", true) -> R.drawable.ic_celular
        cat.contains("Port", true) -> R.drawable.ic_portatil
        cat.contains("Audio", true) -> R.drawable.ic_audifonos
        cat.contains("vestible", true) -> R.drawable.ic_reloj
        else -> R.drawable.ic_bolsa
    }

    /** Redimensiona a máx. 1600 px y recomprime a JPEG (esto elimina los metadatos EXIF). */
    fun guardar(ctx: Context, bmp: Bitmap, prefijo: String): String {
        val esc = minOf(1f, 1600f / maxOf(bmp.width, bmp.height))
        val b = if (esc < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * esc).toInt(), (bmp.height * esc).toInt(), true) else bmp
        val dir = File(ctx.filesDir, "imagenes").apply { mkdirs() }
        val f = File(dir, "${prefijo}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(f).use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return f.absolutePath
    }

    fun desdeUri(ctx: Context, uri: Uri): Bitmap? = try {
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })
        }
    } catch (e: Exception) { null }

    fun mostrar(iv: ImageView, ruta: String?, categoria: String? = null, placeholder: Int? = null, padding: Int = 12) {
        val ctx = iv.context
        val bmp = if (!ruta.isNullOrBlank() && File(ruta).exists())
            BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = 2 }) else null
        if (bmp != null) {
            iv.imageTintList = null
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setPadding(0, 0, 0, 0)
            iv.setImageBitmap(bmp)
        } else {
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            val p = ctx.dp(padding)
            iv.setPadding(p, p, p, p)
            iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.azul_barra))
            iv.setImageResource(placeholder ?: iconoCategoria(categoria ?: ""))
        }
    }
}

/** Cámara (con permiso en tiempo de ejecución) o galería. Se instancia como propiedad de Activity/Fragment. */
class SelectorFoto(
    caller: ActivityResultCaller,
    private val ctx: () -> Context,
    private val prefijo: String,
    private val alObtener: (String) -> Unit
) {
    private val tomar = caller.registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) alObtener(Imagenes.guardar(ctx(), bmp, prefijo))
    }
    private val elegir = caller.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) Imagenes.desdeUri(ctx(), uri)?.let { alObtener(Imagenes.guardar(ctx(), it, prefijo)) }
    }
    private val permiso = caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) tomar.launch(null) else ctx().toast("Sin permiso de cámara: puedes elegir una imagen de la galería")
    }

    fun camara() {
        if (ContextCompat.checkSelfPermission(ctx(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) tomar.launch(null)
        else permiso.launch(Manifest.permission.CAMERA)
    }

    fun galeria() = elegir.launch("image/*")

    fun dialogo() {
        MaterialAlertDialogBuilder(ctx()).setTitle("Foto")
            .setItems(arrayOf("Tomar foto", "Elegir de la galería")) { _, i -> if (i == 0) camara() else galeria() }.show()
    }
}

// ------------------------------------------------------------------ ubicación
data class UbicacionRes(val latitud: Double, val longitud: Double, val calle: String, val ciudad: String)

object Ubicacion {
    fun tienePermiso(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permisos = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    /** Última ubicación conocida + geocodificación inversa. Devuelve null si no hay ubicación disponible. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun resolver(ctx: Context, alTerminar: (UbicacionRes?) -> Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = try {
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        } catch (e: SecurityException) { null }
        if (loc == null) { alTerminar(null); return }
        val main = Handler(Looper.getMainLooper())
        Thread {
            var calle = ""
            var ciudad = ""
            try {
                val l = Geocoder(ctx, Locale("es", "CO")).getFromLocation(loc.latitude, loc.longitude, 1)
                if (!l.isNullOrEmpty()) {
                    calle = l[0].getAddressLine(0) ?: ""
                    ciudad = l[0].locality ?: ""
                }
            } catch (e: Exception) { /* sin red o sin Geocoder: se devuelven solo las coordenadas */ }
            main.post { alTerminar(UbicacionRes(loc.latitude, loc.longitude, calle, ciudad)) }
        }.start()
    }
}

// ------------------------------------------------------------------ biometría
object Biometria {
    fun disponible(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun pedir(act: FragmentActivity, titulo: String, alFallar: () -> Unit = {}, alExito: () -> Unit) {
        val prompt = BiometricPrompt(act, ContextCompat.getMainExecutor(act), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { alExito() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                alFallar()
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    act.toast(errString.toString())
                }
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle("Confirma tu identidad con tu huella")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }
}
