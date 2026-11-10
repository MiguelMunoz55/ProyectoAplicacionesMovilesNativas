# SpecHub — Frontend Android (Actividad 3)

Tienda virtual tipo mercado de tecnología (Android nativo, Kotlin, XML + ViewBinding, Material 3).
Interfaces completas para los roles **Comprador**, **Vendedor** y **Administrador** más las pantallas transversales.
Base de datos local **SQLite**.

## Abrir y ejecutar
1. Android Studio (Koala o superior) → *Open* → carpeta `SpecHub`. Esperar el *Gradle Sync* (JDK 17).
2. Ejecutar en emulador/dispositivo (Android 8.0+).

## Generar el APK para validación
- Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
  Resultado: `app/build/outputs/apk/debug/app-debug.apk`.
- Terminal: `./gradlew assembleDebug`

## Cuentas de prueba
| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@spechub.co | Admin123 |
| Vendedor | vendedor@tecnobog.co | Vende123 |
| Comprador | comprador@correo.com | Compra123 |

## Pantallas por rol
- **Transversal:** Splash, Login (correo, Google*, huella), Registro (cámara, GPS, consentimiento Ley 1581), Recuperar contraseña, navegación inferior por rol, Perfil.
- **Comprador:** Inicio, Catálogo (búsqueda y filtros), Detalle, Carrito, Pasarela de pagos*, Mis compras (seguimiento y retracto), Perfil, Dirección con geolocalización.
- **Vendedor:** Panel (ventas, pedidos, stock bajo), CRUD de productos con cámara, Pedidos recibidos, Mi tienda.
- **Administrador:** Dashboard, Usuarios (listar, crear, editar, suspender, eliminar lógico), Moderación de productos, Reporte de ventas/comisiones (exporta CSV).

\* Simulados en esta fase (ver "Alcance").

## Base de datos (SQLite, `spechub.db`)
`usuarios`, `categorias`, `productos`, `carrito`, `pedidos`, `detalle_pedido`, `direcciones`,
`solicitudes_retracto`, `auditoria`. El esquema está en `data/DbHelper.kt` y todo el acceso en `data/Repositorio.kt`.
Se siembran datos de ejemplo la primera vez. Para reiniciarlos: desinstalar la app o *Borrar datos*.

## Estructura
```
app/src/main/java/co/edu/ucompensar/spechub/
  data/   Modelos, DbHelper (esquema + semillas), Repositorio (consultas)
  util/   Utils (formato, imágenes, cámara, ubicación, biometría), Sesion
  ui/     Splash, Login, Registro, Recuperar, PrincipalActivity (base)
  ui/comprador | ui/vendedor | ui/admin | ui/comun
app/src/main/res/  layout, values (colors, themes, styles, strings), drawable, menu
```


# ProyectoAplicacionesMovilesNativas
