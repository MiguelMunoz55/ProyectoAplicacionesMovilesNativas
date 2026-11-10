package co.edu.ucompensar.spechub.data

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import co.edu.ucompensar.spechub.util.Fmt

/**
 * Base de datos SQLite local de SpecHub..
 * Los datos de ejemplo permiten navegar toda la interfaz sin backend.
 */
class DbHelper(ctx: Context) : SQLiteOpenHelper(ctx, "spechub.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        DDL.forEach { db.execSQL(it) }
        sembrar(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) {
        // Migraciones futuras (RNF-ESC-03). Versión 1: nada que migrar.
    }

    companion object {
        private val DDL = listOf(
            """CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                correo TEXT NOT NULL UNIQUE,
                telefono TEXT NOT NULL DEFAULT '',
                contrasena_hash TEXT NOT NULL,
                rol TEXT NOT NULL CHECK (rol IN ('comprador','vendedor','administrador')),
                estado TEXT NOT NULL DEFAULT 'activo',
                url_foto TEXT,
                nit TEXT NOT NULL DEFAULT '',
                ciudad TEXT NOT NULL DEFAULT '',
                medio_cobro TEXT NOT NULL DEFAULT '',
                version_consentimiento TEXT,
                fecha_consentimiento TEXT,
                eliminado INTEGER NOT NULL DEFAULT 0,
                fecha_creacion TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )""",
            """CREATE TABLE categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE
            )""",
            """CREATE TABLE productos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_vendedor INTEGER NOT NULL REFERENCES usuarios(id),
                id_categoria INTEGER NOT NULL REFERENCES categorias(id),
                titulo TEXT NOT NULL,
                descripcion TEXT NOT NULL DEFAULT '',
                marca TEXT NOT NULL DEFAULT '',
                modelo TEXT NOT NULL DEFAULT '',
                precio INTEGER NOT NULL CHECK (precio >= 0),
                stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
                condicion TEXT NOT NULL DEFAULT 'Nuevo',
                meses_garantia INTEGER NOT NULL DEFAULT 0,
                ram TEXT NOT NULL DEFAULT '',
                almacenamiento TEXT NOT NULL DEFAULT '',
                bateria TEXT NOT NULL DEFAULT '',
                imei TEXT NOT NULL DEFAULT '',
                url_imagen TEXT,
                estado TEXT NOT NULL DEFAULT 'pendiente',
                motivo_rechazo TEXT NOT NULL DEFAULT '',
                eliminado INTEGER NOT NULL DEFAULT 0,
                fecha_creacion TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                fecha_actualizacion TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )""",
            """CREATE TABLE carrito (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario INTEGER NOT NULL REFERENCES usuarios(id),
                id_producto INTEGER NOT NULL REFERENCES productos(id),
                cantidad INTEGER NOT NULL CHECK (cantidad > 0),
                fecha_agregado TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                UNIQUE (id_usuario, id_producto)
            )""",
            """CREATE TABLE pedidos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_comprador INTEGER NOT NULL REFERENCES usuarios(id),
                subtotal INTEGER NOT NULL,
                envio INTEGER NOT NULL,
                iva INTEGER NOT NULL,
                total INTEGER NOT NULL,
                estado TEXT NOT NULL DEFAULT 'pendiente',
                direccion TEXT NOT NULL DEFAULT '',
                medio_pago TEXT NOT NULL DEFAULT '',
                referencia_pago TEXT NOT NULL DEFAULT '',
                fecha_creacion TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )""",
            """CREATE TABLE detalle_pedido (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_pedido INTEGER NOT NULL REFERENCES pedidos(id),
                id_producto INTEGER NOT NULL REFERENCES productos(id),
                id_vendedor INTEGER NOT NULL REFERENCES usuarios(id),
                cantidad INTEGER NOT NULL,
                precio_unitario INTEGER NOT NULL
            )""",
            """CREATE TABLE direcciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario INTEGER NOT NULL REFERENCES usuarios(id),
                calle TEXT NOT NULL,
                ciudad TEXT NOT NULL DEFAULT '',
                referencia TEXT NOT NULL DEFAULT '',
                latitud REAL NOT NULL DEFAULT 0,
                longitud REAL NOT NULL DEFAULT 0,
                es_principal INTEGER NOT NULL DEFAULT 1,
                fecha_creacion TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )""",
            """CREATE TABLE solicitudes_retracto (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_pedido INTEGER NOT NULL REFERENCES pedidos(id),
                motivo TEXT NOT NULL,
                estado TEXT NOT NULL DEFAULT 'pendiente',
                fecha_solicitud TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )""",
            """CREATE TABLE auditoria (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_actor INTEGER NOT NULL,
                accion TEXT NOT NULL,
                objeto TEXT NOT NULL,
                valor_anterior TEXT NOT NULL DEFAULT '',
                valor_nuevo TEXT NOT NULL DEFAULT '',
                fecha TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )"""
        )
    }

    // ---------------------------------------------------------------- datos de ejemplo
    private fun sembrar(db: SQLiteDatabase) {
        usuario(db, "Administrador SpecHub", "admin@spechub.co", "3001112233", "Admin123", "administrador")
        usuario(db, "TecnoBog S.A.S.", "vendedor@tecnobog.co", "3105556677", "Vende123", "vendedor", nit = "901.234.567-8", medio = "Cuenta Bancolombia ****4821")
        usuario(db, "Juan Pérez", "comprador@correo.com", "3001234567", "Compra123", "comprador")
        usuario(db, "María Gómez", "maria@correo.com", "3157778899", "Compra123", "comprador", ciudad = "Medellín")
        usuario(db, "Luis Mora", "luis@compumarket.co", "3209990011", "Vende123", "vendedor", estado = "suspendido", nit = "900.111.222-3", ciudad = "Cali")
        usuario(db, "Ana Ríos", "ana@spechub.co", "3012223344", "Admin123", "administrador")

        listOf("Smartphones", "Portátiles", "Componentes PC", "Audio", "Dispositivos vestibles", "Accesorios")
            .forEach { db.execSQL("INSERT INTO categorias(nombre) VALUES(?)", arrayOf<Any>(it)) }

        //        vend cat titulo                          marca     modelo    precio   stock cond            gar ram     alm      bat       imei               estado
        producto(db, 2, 1, "Smartphone Nova X 8/256 GB", "Nova", "X", 1899000, 12, "Nuevo", 12, "8 GB", "256 GB", "5.000 mAh", "356938035643809", "activo")
        producto(db, 2, 2, "Portátil Zenbo 14\"", "Zenbo", "14", 2450000, 5, "Nuevo", 12, "16 GB", "512 GB SSD", "", "", "activo")
        producto(db, 2, 4, "Audífonos BT Pro", "SoundX", "BT Pro", 189000, 30, "Nuevo", 6, "", "", "40 h", "", "activo")
        producto(db, 2, 1, "Smartphone Lite 6/128", "Nova", "Lite", 899000, 0, "Nuevo", 12, "6 GB", "128 GB", "4.500 mAh", "356938035640000", "activo")
        producto(db, 5, 6, "Teclado mecánico K2", "Keyra", "K2", 329000, 20, "Nuevo", 12, "", "", "", "", "activo")
        producto(db, 5, 5, "Smartwatch Fit 3", "Fitly", "Fit 3", 459000, 15, "Nuevo", 12, "", "", "7 días", "", "activo")
        producto(db, 5, 3, "SSD NVMe 1 TB", "Vortex", "N1000", 289000, 40, "Nuevo", 36, "", "1 TB", "", "", "activo")
        producto(db, 2, 4, "Audífonos Studio ANC", "SoundX", "Studio ANC", 520000, 10, "Nuevo", 12, "", "", "30 h", "", "pendiente")
        producto(db, 5, 1, "Tablet Orion 10", "Orion", "10", 780000, 8, "Nuevo", 12, "4 GB", "64 GB", "6.000 mAh", "", "pendiente")
        producto(db, 5, 2, "Portátil ThinkBook reacondicionado", "Lenovo", "ThinkBook 14", 1350000, 3, "Reacondicionado", 6, "8 GB", "256 GB SSD", "", "", "activo")

        val dirJuan = "Cra 7 # 72-41, Bogotá"
        pedido(db, 3, "enviado", "-2 days", dirJuan, "Tarjeta", listOf(Triple(1L, 1, 1899000L), Triple(2L, 1, 2450000L), Triple(3L, 1, 189000L)))
        pedido(db, 3, "entregado", "-9 days", dirJuan, "PSE", listOf(Triple(5L, 1, 329000L)))
        pedido(db, 3, "pagado", "-1 days", dirJuan, "Nequi", listOf(Triple(6L, 1, 459000L)))
        pedido(db, 3, "cancelado", "-5 days", dirJuan, "Tarjeta", listOf(Triple(7L, 1, 289000L)))
        pedido(db, 4, "pagado", "0 days", "Cll 10 # 40-12, Medellín", "Tarjeta", listOf(Triple(3L, 1, 189000L)))
        pedido(db, 4, "pagado", "-3 days", "Cll 10 # 40-12, Medellín", "Bancolombia", listOf(Triple(10L, 1, 1350000L)))
        pedido(db, 4, "entregado", "-4 days", "Cll 10 # 40-12, Medellín", "Tarjeta", listOf(Triple(5L, 2, 329000L)))
        pedido(db, 3, "entregado", "-6 days", dirJuan, "Nequi", listOf(Triple(3L, 2, 189000L)))

        db.execSQL(
            "INSERT INTO direcciones(id_usuario,calle,ciudad,referencia,latitud,longitud,es_principal) VALUES(3,'Cra 7 # 72-41, Chapinero','Bogotá','Torre 2, Apto 504',4.6586,-74.0600,1)"
        )
    }

    private fun usuario(
        db: SQLiteDatabase, nombre: String, correo: String, tel: String, clave: String, rol: String,
        estado: String = "activo", nit: String = "", ciudad: String = "Bogotá", medio: String = ""
    ) {
        db.execSQL(
            "INSERT INTO usuarios(nombre,correo,telefono,contrasena_hash,rol,estado,nit,ciudad,medio_cobro,version_consentimiento,fecha_consentimiento) " +
                "VALUES(?,?,?,?,?,?,?,?,?,'v1.0',datetime('now','localtime'))",
            arrayOf<Any>(nombre, correo, tel, Fmt.sha256(clave), rol, estado, nit, ciudad, medio)
        )
    }

    private fun producto(
        db: SQLiteDatabase, idV: Long, idCat: Long, titulo: String, marca: String, modelo: String, precio: Long,
        stock: Int, cond: String, garantia: Int, ram: String, alm: String, bat: String, imei: String, estado: String
    ) {
        db.execSQL(
            "INSERT INTO productos(id_vendedor,id_categoria,titulo,descripcion,marca,modelo,precio,stock,condicion,meses_garantia,ram,almacenamiento,bateria,imei,estado) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            arrayOf<Any>(
                idV, idCat, titulo, "$titulo. Producto con ficha técnica verificada por SpecHub, garantía del vendedor y soporte en Colombia.",
                marca, modelo, precio, stock, cond, garantia, ram, alm, bat, imei, estado
            )
        )
    }

    private fun pedido(
        db: SQLiteDatabase, idComprador: Long, estado: String, hace: String, direccion: String, medio: String,
        items: List<Triple<Long, Int, Long>>
    ) {
        val subtotal = items.sumOf { it.second * it.third }
        val envio = Repositorio.ENVIO
        val iva = Math.round(subtotal * 0.19 / 1.19)
        db.execSQL(
            "INSERT INTO pedidos(id_comprador,subtotal,envio,iva,total,estado,direccion,medio_pago,referencia_pago,fecha_creacion) " +
                "VALUES(?,?,?,?,?,?,?,?,?,datetime('now','localtime',?))",
            arrayOf<Any>(idComprador, subtotal, envio, iva, subtotal + envio, estado, direccion, medio, "SPH-SEED-${System.nanoTime()}", hace)
        )
        val idPedido = DatabaseUtils.longForQuery(db, "SELECT last_insert_rowid()", null)
        items.forEach { (idProd, cant, precio) ->
            val idVend = DatabaseUtils.longForQuery(db, "SELECT id_vendedor FROM productos WHERE id=?", arrayOf(idProd.toString()))
            db.execSQL(
                "INSERT INTO detalle_pedido(id_pedido,id_producto,id_vendedor,cantidad,precio_unitario) VALUES(?,?,?,?,?)",
                arrayOf<Any>(idPedido, idProd, idVend, cant, precio)
            )
        }
    }
}
