package co.edu.ucompensar.spechub.data

data class Usuario(
    val id: Long,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val rol: String,          // comprador | vendedor | administrador
    val estado: String,       // activo | suspendido
    val urlFoto: String?,
    val nit: String,
    val ciudad: String,
    val medioCobro: String
)

data class Categoria(val id: Long, val nombre: String)

data class Producto(
    val id: Long = 0,
    val idVendedor: Long = 0,
    val vendedor: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val idCategoria: Long = 0,
    val categoria: String = "",
    val marca: String = "",
    val modelo: String = "",
    val precio: Long = 0,
    val stock: Int = 0,
    val condicion: String = "Nuevo",
    val mesesGarantia: Int = 12,
    val ram: String = "",
    val almacenamiento: String = "",
    val bateria: String = "",
    val imei: String = "",
    val urlImagen: String? = null,
    val estado: String = "pendiente",   // pendiente | activo | rechazado | suspendido
    val motivoRechazo: String = ""
)

data class ItemCarrito(
    val id: Long,
    val idProducto: Long,
    val idVendedor: Long,
    val titulo: String,
    val precio: Long,
    val cantidad: Int,
    val stock: Int,
    val urlImagen: String?,
    val categoria: String
)

data class Totales(val subtotal: Long, val envio: Long, val iva: Long, val total: Long)

data class Pedido(
    val id: Long,
    val comprador: String,
    val total: Long,
    val estado: String,   // pendiente | pagado | enviado | entregado | cancelado
    val direccion: String,
    val medioPago: String,
    val fecha: String,
    val resumen: String,
    val retractoSolicitado: Boolean
) {
    val codigo: String get() = "#SH-${20000 + id}"
}

data class Direccion(
    val id: Long,
    val idUsuario: Long,
    val calle: String,
    val ciudad: String,
    val referencia: String,
    val latitud: Double,
    val longitud: Double
) {
    val texto: String get() = listOf(calle, ciudad).filter { it.isNotBlank() }.joinToString(", ")
}

data class ResumenAdmin(val usuarios: Int, val vendedores: Int, val ventasHoy: Long, val pendientes: Int)

data class ReporteVentas(val comision: Long, val transacciones: Int, val aprobadasPct: Int, val filas: List<Pedido>)
