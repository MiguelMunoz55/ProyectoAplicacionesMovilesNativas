package co.edu.ucompensar.spechub.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import co.edu.ucompensar.spechub.util.Fmt
import java.util.Calendar
import kotlin.math.roundToLong

private fun Cursor.str(c: String): String = getString(getColumnIndexOrThrow(c)) ?: ""
private fun Cursor.strN(c: String): String? = getString(getColumnIndexOrThrow(c))
private fun Cursor.lng(c: String): Long = getLong(getColumnIndexOrThrow(c))
private fun Cursor.int(c: String): Int = getInt(getColumnIndexOrThrow(c))

private fun Cursor.aUsuario() = Usuario(
    lng("id"), str("nombre"), str("correo"), str("telefono"), str("rol"), str("estado"),
    strN("url_foto"), str("nit"), str("ciudad"), str("medio_cobro")
)

private fun Cursor.aProducto() = Producto(
    lng("id"), lng("id_vendedor"), str("vendedor"), str("titulo"), str("descripcion"),
    lng("id_categoria"), str("categoria"), str("marca"), str("modelo"), lng("precio"), int("stock"),
    str("condicion"), int("meses_garantia"), str("ram"), str("almacenamiento"), str("bateria"),
    str("imei"), strN("url_imagen"), str("estado"), str("motivo_rechazo")
)

/**
 * Acceso a datos de SpecHub (SQLite). Toda la lógica de persistencia vive aquí para que la
 * interfaz no conozca el esquema; cuando exista backend solo se reemplaza este archivo.
 */
object Repositorio {
    const val ENVIO = 15000L
    const val COMISION = 0.05
    private const val ESTADOS_VENTA = "'pagado','enviado','entregado'"

    private lateinit var helper: DbHelper
    fun init(c: Context) {
        if (!::helper.isInitialized) helper = DbHelper(c.applicationContext)
    }

    private val db: SQLiteDatabase get() = helper.writableDatabase

    // ================================================================= usuarios
    fun autenticar(correo: String, clave: String): Usuario? =
        db.rawQuery(
            "SELECT * FROM usuarios WHERE correo=? AND contrasena_hash=? AND eliminado=0",
            arrayOf(correo.trim().lowercase(), Fmt.sha256(clave))
        ).use { if (it.moveToFirst()) it.aUsuario() else null }

    fun usuarioPorId(id: Long): Usuario? =
        db.rawQuery("SELECT * FROM usuarios WHERE id=? AND eliminado=0", arrayOf(id.toString()))
            .use { if (it.moveToFirst()) it.aUsuario() else null }

    fun crearUsuario(
        nombre: String, correo: String, telefono: String, clave: String, rol: String,
        nit: String = "", ciudad: String = "", urlFoto: String? = null
    ): Long {
        val cv = ContentValues().apply {
            put("nombre", nombre.trim())
            put("correo", correo.trim().lowercase())
            put("telefono", telefono)
            put("contrasena_hash", Fmt.sha256(clave))
            put("rol", rol)
            put("nit", nit)
            put("ciudad", ciudad)
            put("url_foto", urlFoto)
            put("version_consentimiento", "v1.0")
            put("fecha_consentimiento", Fmt.ahora())
        }
        return try { db.insertOrThrow("usuarios", null, cv) } catch (e: SQLException) { -1L }
    }

    fun actualizarPerfil(id: Long, nombre: String, telefono: String, nit: String, ciudad: String, medioCobro: String) {
        val cv = ContentValues().apply {
            put("nombre", nombre); put("telefono", telefono); put("nit", nit)
            put("ciudad", ciudad); put("medio_cobro", medioCobro)
        }
        db.update("usuarios", cv, "id=?", arrayOf(id.toString()))
    }

    fun actualizarFoto(id: Long, ruta: String) {
        db.update("usuarios", ContentValues().apply { put("url_foto", ruta) }, "id=?", arrayOf(id.toString()))
    }

    fun listarUsuarios(texto: String = "", rol: String = ""): List<Usuario> {
        val w = StringBuilder("eliminado=0")
        val a = mutableListOf<String>()
        if (texto.isNotBlank()) {
            w.append(" AND (nombre LIKE ? OR correo LIKE ?)")
            repeat(2) { a += "%${texto.trim().take(50)}%" }
        }
        if (rol.isNotBlank()) { w.append(" AND rol=?"); a += rol }
        val r = mutableListOf<Usuario>()
        db.rawQuery("SELECT * FROM usuarios WHERE $w ORDER BY id", a.toTypedArray()).use { while (it.moveToNext()) r += it.aUsuario() }
        return r
    }

    fun actualizarUsuarioAdmin(id: Long, nombre: String, telefono: String, rol: String, idAdmin: Long, motivo: String) {
        val antes = usuarioPorId(id)
        val cv = ContentValues().apply { put("nombre", nombre); put("telefono", telefono); put("rol", rol) }
        db.update("usuarios", cv, "id=?", arrayOf(id.toString()))
        auditar(idAdmin, "actualizar_usuario", "usuario:$id", "${antes?.nombre}|${antes?.rol}", "$nombre|$rol — $motivo")
    }

    fun cambiarEstadoUsuario(id: Long, estado: String, idAdmin: Long, motivo: String) {
        val antes = usuarioPorId(id)?.estado ?: ""
        db.update("usuarios", ContentValues().apply { put("estado", estado) }, "id=?", arrayOf(id.toString()))
        auditar(idAdmin, "cambiar_estado_usuario", "usuario:$id", antes, "$estado — $motivo")
    }

    /** Borrado lógico con anonimización (RF-USR-04, RF-PER-05). */
    fun eliminarUsuario(id: Long, idActor: Long, motivo: String) {
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("nombre", "Usuario eliminado"); put("correo", "eliminado_$id@spechub.invalid")
                put("telefono", ""); putNull("url_foto"); put("eliminado", 1); put("estado", "suspendido")
            }
            db.update("usuarios", cv, "id=?", arrayOf(id.toString()))
            db.delete("carrito", "id_usuario=?", arrayOf(id.toString()))
            db.delete("direcciones", "id_usuario=?", arrayOf(id.toString()))
            auditar(idActor, "eliminar_usuario", "usuario:$id", "activo", "eliminado — $motivo")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun auditar(idActor: Long, accion: String, objeto: String, anterior: String, nuevo: String) {
        db.execSQL(
            "INSERT INTO auditoria(id_actor,accion,objeto,valor_anterior,valor_nuevo) VALUES(?,?,?,?,?)",
            arrayOf<Any>(idActor, accion, objeto, anterior, nuevo)
        )
    }

    // ================================================================= categorías y productos
    fun categorias(): List<Categoria> {
        val r = mutableListOf<Categoria>()
        db.rawQuery("SELECT id, nombre FROM categorias ORDER BY id", null).use { while (it.moveToNext()) r += Categoria(it.getLong(0), it.getString(1)) }
        return r
    }

    private const val SQL_PROD =
        "SELECT p.*, c.nombre AS categoria, u.nombre AS vendedor FROM productos p " +
            "JOIN categorias c ON c.id=p.id_categoria JOIN usuarios u ON u.id=p.id_vendedor"

    private fun consultaProductos(where: String, args: Array<String>, orden: String = "p.id DESC"): List<Producto> {
        val r = mutableListOf<Producto>()
        db.rawQuery("$SQL_PROD WHERE p.eliminado=0 AND $where ORDER BY $orden", args).use { while (it.moveToNext()) r += it.aProducto() }
        return r
    }

    fun buscarProductos(texto: String = "", idCategoria: Long = 0, condicion: String = ""): List<Producto> {
        val w = StringBuilder("p.estado='activo' AND u.estado='activo'")
        val a = mutableListOf<String>()
        if (texto.isNotBlank()) {
            w.append(" AND (p.titulo LIKE ? OR p.marca LIKE ? OR p.modelo LIKE ?)")
            repeat(3) { a += "%${texto.trim().take(50)}%" }
        }
        if (idCategoria > 0) { w.append(" AND p.id_categoria=?"); a += idCategoria.toString() }
        if (condicion.isNotBlank()) { w.append(" AND p.condicion=?"); a += condicion }
        return consultaProductos(w.toString(), a.toTypedArray())
    }

    fun productoPorId(id: Long): Producto? =
        consultaProductos("p.id=?", arrayOf(id.toString())).firstOrNull()

    fun productosVendedor(idVendedor: Long, estado: String = "", texto: String = ""): List<Producto> {
        val w = StringBuilder("p.id_vendedor=?")
        val a = mutableListOf(idVendedor.toString())
        if (estado.isNotBlank()) { w.append(" AND p.estado=?"); a += estado }
        if (texto.isNotBlank()) { w.append(" AND p.titulo LIKE ?"); a += "%${texto.trim().take(50)}%" }
        return consultaProductos(w.toString(), a.toTypedArray())
    }

    fun productosPorEstado(estado: String): List<Producto> =
        consultaProductos("p.estado=?", arrayOf(estado))

    fun guardarProducto(p: Producto): Long {
        val cv = ContentValues().apply {
            put("id_vendedor", p.idVendedor); put("id_categoria", p.idCategoria)
            put("titulo", p.titulo); put("descripcion", p.descripcion)
            put("marca", p.marca); put("modelo", p.modelo)
            put("precio", p.precio); put("stock", p.stock)
            put("condicion", p.condicion); put("meses_garantia", p.mesesGarantia)
            put("ram", p.ram); put("almacenamiento", p.almacenamiento); put("bateria", p.bateria)
            put("imei", p.imei); put("url_imagen", p.urlImagen); put("estado", p.estado)
            put("fecha_actualizacion", Fmt.ahora())
        }
        return if (p.id == 0L) db.insertOrThrow("productos", null, cv)
        else { db.update("productos", cv, "id=?", arrayOf(p.id.toString())); p.id }
    }

    /** Borrado lógico; no se permite si hay pedidos activos asociados (RF-PRD-05). */
    fun eliminarProducto(id: Long): Boolean {
        val activos = DatabaseUtils.longForQuery(
            db,
            "SELECT COUNT(*) FROM detalle_pedido d JOIN pedidos o ON o.id=d.id_pedido " +
                "WHERE d.id_producto=? AND o.estado IN ('pendiente','pagado','enviado')",
            arrayOf(id.toString())
        )
        if (activos > 0) return false
        db.update("productos", ContentValues().apply { put("eliminado", 1) }, "id=?", arrayOf(id.toString()))
        return true
    }

    fun moderarProducto(id: Long, estado: String, motivo: String, idAdmin: Long) {
        val antes = productoPorId(id)?.estado ?: ""
        val cv = ContentValues().apply { put("estado", estado); put("motivo_rechazo", motivo) }
        db.update("productos", cv, "id=?", arrayOf(id.toString()))
        auditar(idAdmin, "moderar_producto", "producto:$id", antes, "$estado — $motivo")
    }

    fun stockBajo(idVendedor: Long, umbral: Int = 5): List<Producto> =
        consultaProductos("p.id_vendedor=? AND p.estado='activo' AND p.stock<=?", arrayOf(idVendedor.toString(), umbral.toString()), "p.stock")

    // ================================================================= carrito
    fun agregarAlCarrito(idUsuario: Long, idProducto: Long, cantidad: Int): String? {
        val p = productoPorId(idProducto) ?: return "El producto ya no está disponible"
        val actual = db.rawQuery(
            "SELECT cantidad FROM carrito WHERE id_usuario=? AND id_producto=?",
            arrayOf(idUsuario.toString(), idProducto.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val nueva = actual + cantidad
        if (p.stock <= 0) return "Producto agotado"
        if (nueva > p.stock) return "Solo hay ${p.stock} unidades disponibles"
        if (nueva > 10) return "Máximo 10 unidades por producto"
        if (actual == 0) {
            db.insert("carrito", null, ContentValues().apply {
                put("id_usuario", idUsuario); put("id_producto", idProducto); put("cantidad", cantidad)
            })
        } else {
            db.update("carrito", ContentValues().apply { put("cantidad", nueva) }, "id_usuario=? AND id_producto=?", arrayOf(idUsuario.toString(), idProducto.toString()))
        }
        return null
    }

    fun listarCarrito(idUsuario: Long): List<ItemCarrito> {
        val r = mutableListOf<ItemCarrito>()
        db.rawQuery(
            "SELECT k.id, k.cantidad, p.id AS id_producto, p.id_vendedor, p.titulo, p.precio, p.stock, p.url_imagen, c.nombre AS categoria " +
                "FROM carrito k JOIN productos p ON p.id=k.id_producto JOIN categorias c ON c.id=p.id_categoria " +
                "WHERE k.id_usuario=? ORDER BY k.id",
            arrayOf(idUsuario.toString())
        ).use {
            while (it.moveToNext()) r += ItemCarrito(
                it.lng("id"), it.lng("id_producto"), it.lng("id_vendedor"), it.str("titulo"),
                it.lng("precio"), it.int("cantidad"), it.int("stock"), it.strN("url_imagen"), it.str("categoria")
            )
        }
        return r
    }

    fun contarCarrito(idUsuario: Long): Int =
        DatabaseUtils.longForQuery(db, "SELECT COALESCE(SUM(cantidad),0) FROM carrito WHERE id_usuario=?", arrayOf(idUsuario.toString())).toInt()

    fun cambiarCantidad(idItem: Long, cantidad: Int): String? {
        val stock = DatabaseUtils.longForQuery(
            db, "SELECT p.stock FROM carrito k JOIN productos p ON p.id=k.id_producto WHERE k.id=?", arrayOf(idItem.toString())
        ).toInt()
        if (cantidad > stock) return "Solo hay $stock unidades disponibles"
        if (cantidad > 10) return "Máximo 10 unidades por producto"
        db.update("carrito", ContentValues().apply { put("cantidad", cantidad) }, "id=?", arrayOf(idItem.toString()))
        return null
    }

    fun quitarItem(idItem: Long) { db.delete("carrito", "id=?", arrayOf(idItem.toString())) }

    fun totales(items: List<ItemCarrito>): Totales {
        val subtotal = items.sumOf { it.precio * it.cantidad }
        val envio = if (items.isEmpty()) 0L else ENVIO
        val iva = (subtotal * 0.19 / 1.19).roundToLong()   // IVA incluido en el precio
        return Totales(subtotal, envio, iva, subtotal + envio)
    }

    // ================================================================= direcciones
    fun direccionPrincipal(idUsuario: Long): Direccion? =
        db.rawQuery(
            "SELECT * FROM direcciones WHERE id_usuario=? ORDER BY es_principal DESC, id DESC LIMIT 1",
            arrayOf(idUsuario.toString())
        ).use {
            if (it.moveToFirst()) Direccion(it.lng("id"), it.lng("id_usuario"), it.str("calle"), it.str("ciudad"), it.str("referencia"),
                it.getDouble(it.getColumnIndexOrThrow("latitud")), it.getDouble(it.getColumnIndexOrThrow("longitud"))) else null
        }

    fun guardarDireccion(d: Direccion) {
        val cv = ContentValues().apply {
            put("id_usuario", d.idUsuario); put("calle", d.calle); put("ciudad", d.ciudad)
            put("referencia", d.referencia); put("latitud", d.latitud); put("longitud", d.longitud); put("es_principal", 1)
        }
        if (d.id == 0L) db.insertOrThrow("direcciones", null, cv) else db.update("direcciones", cv, "id=?", arrayOf(d.id.toString()))
    }

    // ================================================================= pedidos
    /** Crea el pedido desde el carrito en una sola transacción: descuenta stock y vacía el carrito. */
    fun crearPedido(idUsuario: Long, direccion: String, medioPago: String): Long {
        val items = listarCarrito(idUsuario)
        if (items.isEmpty()) return -1
        val t = totales(items)
        db.beginTransaction()
        try {
            val st = db.compileStatement("UPDATE productos SET stock=stock-? WHERE id=? AND stock>=?")
            for (i in items) {
                st.clearBindings()
                st.bindLong(1, i.cantidad.toLong()); st.bindLong(2, i.idProducto); st.bindLong(3, i.cantidad.toLong())
                if (st.executeUpdateDelete() == 0) return -1
            }
            val cv = ContentValues().apply {
                put("id_comprador", idUsuario); put("subtotal", t.subtotal); put("envio", t.envio); put("iva", t.iva)
                put("total", t.total); put("estado", "pagado"); put("direccion", direccion); put("medio_pago", medioPago)
                put("referencia_pago", "SPH-${System.currentTimeMillis()}")
            }
            val idPedido = db.insertOrThrow("pedidos", null, cv)
            for (i in items) {
                db.insertOrThrow("detalle_pedido", null, ContentValues().apply {
                    put("id_pedido", idPedido); put("id_producto", i.idProducto); put("id_vendedor", i.idVendedor)
                    put("cantidad", i.cantidad); put("precio_unitario", i.precio)
                })
            }
            db.delete("carrito", "id_usuario=?", arrayOf(idUsuario.toString()))
            db.setTransactionSuccessful()
            return idPedido
        } finally { db.endTransaction() }
    }

    private const val SQL_PEDIDO =
        "SELECT o.*, u.nombre AS comprador, " +
            "(SELECT COUNT(*) FROM solicitudes_retracto r WHERE r.id_pedido=o.id) AS n_retracto " +
            "FROM pedidos o JOIN usuarios u ON u.id=o.id_comprador"

    private fun Cursor.aPedido() = Pedido(
        lng("id"), str("comprador"), lng("total"), str("estado"), str("direccion"), str("medio_pago"),
        str("fecha_creacion"), resumen(lng("id")), int("n_retracto") > 0
    )

    private fun resumen(idPedido: Long, idVendedor: Long = 0): String {
        val titulos = mutableListOf<String>()
        val sql = "SELECT p.titulo FROM detalle_pedido d JOIN productos p ON p.id=d.id_producto WHERE d.id_pedido=?" +
            (if (idVendedor > 0) " AND d.id_vendedor=?" else "") + " ORDER BY d.id"
        val args = if (idVendedor > 0) arrayOf(idPedido.toString(), idVendedor.toString()) else arrayOf(idPedido.toString())
        db.rawQuery(sql, args).use { while (it.moveToNext()) titulos += it.getString(0) }
        return when {
            titulos.isEmpty() -> ""
            titulos.size == 1 -> titulos[0]
            else -> "${titulos[0]} + ${titulos.size - 1} producto${if (titulos.size > 2) "s" else ""}"
        }
    }

    fun pedidosComprador(idUsuario: Long, estados: List<String>): List<Pedido> {
        val marcas = estados.joinToString(",") { "'$it'" }
        val r = mutableListOf<Pedido>()
        db.rawQuery("$SQL_PEDIDO WHERE o.id_comprador=? AND o.estado IN ($marcas) ORDER BY o.id DESC", arrayOf(idUsuario.toString()))
            .use { while (it.moveToNext()) r += it.aPedido() }
        return r
    }

    fun pedidosVendedor(idVendedor: Long, estados: List<String>): List<Pedido> {
        val marcas = estados.joinToString(",") { "'$it'" }
        val filas = mutableListOf<Pedido>()
        db.rawQuery(
            "SELECT o.id, o.estado, o.direccion, o.medio_pago, o.fecha_creacion, u.nombre AS comprador, " +
                "SUM(d.cantidad*d.precio_unitario) AS total_v " +
                "FROM pedidos o JOIN detalle_pedido d ON d.id_pedido=o.id JOIN usuarios u ON u.id=o.id_comprador " +
                "WHERE d.id_vendedor=? AND o.estado IN ($marcas) GROUP BY o.id ORDER BY o.id DESC",
            arrayOf(idVendedor.toString())
        ).use {
            while (it.moveToNext()) filas += Pedido(
                it.lng("id"), it.str("comprador"), it.lng("total_v"), it.str("estado"), it.str("direccion"),
                it.str("medio_pago"), it.str("fecha_creacion"), "", false
            )
        }
        return filas.map { it.copy(resumen = resumen(it.id, idVendedor)) }
    }

    fun cambiarEstadoPedido(idPedido: Long, estado: String) {
        db.update("pedidos", ContentValues().apply { put("estado", estado) }, "id=?", arrayOf(idPedido.toString()))
    }

    fun detallePedido(idPedido: Long): List<String> {
        val r = mutableListOf<String>()
        db.rawQuery(
            "SELECT d.cantidad, d.precio_unitario, p.titulo FROM detalle_pedido d JOIN productos p ON p.id=d.id_producto WHERE d.id_pedido=? ORDER BY d.id",
            arrayOf(idPedido.toString())
        ).use { while (it.moveToNext()) r += "${it.getInt(0)} × ${it.getString(2)} — ${Fmt.cop(it.getLong(1) * it.getInt(0))}" }
        return r
    }

    fun solicitarRetracto(idPedido: Long, motivo: String) {
        db.insert("solicitudes_retracto", null, ContentValues().apply { put("id_pedido", idPedido); put("motivo", motivo) })
    }

    // ================================================================= estadísticas
    fun ventasMesVendedor(idVendedor: Long): Long = DatabaseUtils.longForQuery(
        db,
        "SELECT COALESCE(SUM(d.cantidad*d.precio_unitario),0) FROM detalle_pedido d JOIN pedidos o ON o.id=d.id_pedido " +
            "WHERE d.id_vendedor=? AND o.estado IN ($ESTADOS_VENTA) AND strftime('%Y-%m',o.fecha_creacion)=strftime('%Y-%m','now','localtime')",
        arrayOf(idVendedor.toString())
    )

    fun pedidosNuevosVendedor(idVendedor: Long): Int = DatabaseUtils.longForQuery(
        db,
        "SELECT COUNT(DISTINCT o.id) FROM pedidos o JOIN detalle_pedido d ON d.id_pedido=o.id WHERE d.id_vendedor=? AND o.estado='pagado'",
        arrayOf(idVendedor.toString())
    ).toInt()

    /** Ventas por día de los últimos [dias] días (idVendedor = 0 → toda la plataforma). */
    fun ventasUltimosDias(idVendedor: Long = 0, dias: Int = 7): List<Pair<String, Long>> {
        val letras = arrayOf("D", "L", "M", "M", "J", "V", "S")
        val r = mutableListOf<Pair<String, Long>>()
        for (d in dias - 1 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }
            val etiqueta = letras[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val arg = "-$d days"
            val total = if (idVendedor > 0) DatabaseUtils.longForQuery(
                db,
                "SELECT COALESCE(SUM(d.cantidad*d.precio_unitario),0) FROM detalle_pedido d JOIN pedidos o ON o.id=d.id_pedido " +
                    "WHERE d.id_vendedor=? AND o.estado IN ($ESTADOS_VENTA) AND date(o.fecha_creacion)=date('now','localtime',?)",
                arrayOf(idVendedor.toString(), arg)
            ) else DatabaseUtils.longForQuery(
                db,
                "SELECT COALESCE(SUM(total),0) FROM pedidos WHERE estado IN ($ESTADOS_VENTA) AND date(fecha_creacion)=date('now','localtime',?)",
                arrayOf(arg)
            )
            r += etiqueta to total
        }
        return r
    }

    fun resumenAdmin(): ResumenAdmin = ResumenAdmin(
        DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM usuarios WHERE eliminado=0 AND estado='activo'", null).toInt(),
        DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM usuarios WHERE eliminado=0 AND rol='vendedor'", null).toInt(),
        DatabaseUtils.longForQuery(
            db, "SELECT COALESCE(SUM(total),0) FROM pedidos WHERE estado IN ($ESTADOS_VENTA) AND date(fecha_creacion)=date('now','localtime')", null
        ),
        DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM productos WHERE eliminado=0 AND estado='pendiente'", null).toInt()
    )

    fun reporteVentas(): ReporteVentas {
        val filas = mutableListOf<Pedido>()
        db.rawQuery("$SQL_PEDIDO ORDER BY o.id DESC", null).use { while (it.moveToNext()) filas += it.aPedido() }
        val mes = DatabaseUtils.longForQuery(
            db,
            "SELECT COALESCE(SUM(total),0) FROM pedidos WHERE estado IN ($ESTADOS_VENTA) AND strftime('%Y-%m',fecha_creacion)=strftime('%Y-%m','now','localtime')",
            null
        )
        val aprobadas = filas.count { it.estado != "cancelado" && it.estado != "pendiente" }
        val pct = if (filas.isEmpty()) 0 else aprobadas * 100 / filas.size
        return ReporteVentas((mes * COMISION).roundToLong(), filas.size, pct, filas)
    }
}
