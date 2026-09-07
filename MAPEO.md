# Guía de mapeo JPA — autosusbcali

Material de repaso del mapeo del modelo de dominio contra `servicios_db` (PostgreSQL).
Aquí está **qué se usó, por qué, y qué se decidió no usar**.

---

## 1. Inventario: tabla ↔ clase

La base de datos tiene **33 tablas**. El modelo tiene **31 clases**: las tablas
`usuarios_roles` y `roles_permisos` no tienen clase propia porque son puentes de dos FK
(se resuelven con `@ManyToMany`, ver §5).

| Tabla | Clase | Estado |
|---|---|---|
| `marcas` | `Marca` | ✔ |
| `modelos` | `Modelo` | ✔ |
| `vehiculos` | `Vehiculo` | ✔ |
| `fichas_tecnicas` | `FichaTecnica` | ✔ |
| `imagenes_vehiculo` | `ImagenVehiculo` | ✔ |
| `documentos_vehiculo` | `DocumentoVehiculo` | ✔ |
| `verificaciones_runt` | `VerificacionRunt` | ✔ |
| `usuarios` | `Usuario` | ✔ |
| `roles` | `Rol` | ✔ |
| `permisos` | `Permiso` | ✔ |
| `usuarios_roles` | — | `@ManyToMany` en `Usuario` |
| `roles_permisos` | — | `@ManyToMany` en `Rol` |
| `oportunidades_venta` | `OportunidadVenta` | ✔ |
| `negociaciones` | `Negociacion` | ✔ |
| `reservas` | `Reserva` | ✔ |
| `citas_servicio` | `CitaServicio` | ✔ |
| `cotizaciones` | `Cotizacion` | ✔ |
| `accesorios_cotizacion` | `AccesorioCotizacion` | ✔ |
| `entidades_financieras` | `EntidadFinanciera` | ✔ |
| `solicitudes_credito` | `SolicitudCredito` | ✔ |
| `ventas` | `Venta` | ✔ |
| `pagos` | `Pago` | ✔ |
| `transacciones_pasarela` | `TransaccionPasarela` | ✔ |
| `tramites_traspaso` | `TramiteTraspaso` | ✔ |
| `auditoria` | `Auditoria` | ✔ |
| `tokens_recuperacion` | `TokenRecuperacion` | ✔ |
| `busquedas_guardadas` | `BusquedaGuardada` | ✔ |
| `parametros_sistema` | `ParametroSistema` | ✔ |
| `facturas` | `Factura` | ✔ |
| `contratos` | `Contrato` | ✔ |
| `garantias` | `Garantia` | ✔ |
| `pqrs` | `Pqrs` | ✔ |
| `notificaciones` | `Notificacion` | ✔ |

---

## 2. El esqueleto de una entidad

Todas las clases siguen exactamente este patrón:

```java
@Entity
@Table(name = "marcas")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Marca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 100, nullable = false, unique = true)
    private String nombre;
}
```

---

## 3. Anotación por anotación

### De JPA (`jakarta.persistence`)

| Anotación | Qué hace |
|---|---|
| `@Entity` | Marca la clase como entidad persistente. Sin esto, Hibernate la ignora. |
| `@Table(name = "...")` | Dice a qué tabla corresponde. Es necesario porque los nombres no coinciden: la clase se llama `Marca` y la tabla `marcas`. |
| `@Id` | Marca el atributo que es la clave primaria. Toda entidad necesita uno. |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | El valor de la PK lo genera la base de datos, no Java. Corresponde a las columnas con `DEFAULT nextval(...)`, o sea los `BIGSERIAL` de PostgreSQL. |
| `@Column(...)` | Describe la columna. Ver los atributos abajo. |
| `@ManyToOne` | Relación muchos a uno: muchos `Vehiculo` apuntan a un `Modelo`. Es el lado **dueño**: ahí es donde vive la columna FK. |
| `@JoinColumn(name = "...")` | El nombre real de la columna FK en la tabla. |
| `@ManyToMany` | Relación muchos a muchos, resuelta por una tabla puente. |
| `@JoinTable(...)` | Describe esa tabla puente: su nombre y cuál columna apunta a cada lado. |

### Atributos de `@Column`

| Atributo | Para qué | Ejemplo |
|---|---|---|
| `name` | Nombre real de la columna. Se usa **siempre**, porque en Java va camelCase (`fechaCreacion`) y en la BD snake_case (`fecha_creacion`). | `name = "fecha_creacion"` |
| `length` | Largo del `varchar`. | `length = 100` |
| `nullable` | Si la columna acepta `NULL`. `false` significa que la BD tiene `NOT NULL`. | `nullable = false` |
| `precision` / `scale` | Dígitos totales y decimales de un `numeric`. `numeric(15,2)` se mapea con `precision = 15, scale = 2`. | dinero |
| `unique` | La columna tiene restricción `UNIQUE` en la BD. | `email`, `vin` |
| `columnDefinition` | Fuerza el tipo SQL de la columna. Solo se usa para `jsonb`, que Java no tiene. | `columnDefinition = "jsonb"` |

Una columna `text` se mapea con `@Column(name = "...")` **sin `length`**: es texto de
largo libre, no tiene límite que declarar.

### De Lombok

| Anotación | Qué genera |
|---|---|
| `@Data` | Getters, setters, `toString()`, `equals()` y `hashCode()`. |
| `@NoArgsConstructor` | Constructor vacío. **JPA lo exige**: Hibernate crea el objeto sin argumentos y después rellena los campos. Sin él, la entidad falla al arrancar. |
| `@AllArgsConstructor` | Constructor con todos los campos. Comodidad para crear objetos en pruebas. |

---

## 4. Tipos: de PostgreSQL a Java

| Columna PostgreSQL | Java | Ejemplo en el proyecto |
|---|---|---|
| `bigint` (PK) | `Long` | `Marca.id` |
| `bigint` (FK) | la entidad, con `@ManyToOne` | `Vehiculo.modelo` |
| `varchar(n)` | `String` + `length = n` | `Usuario.email` |
| `text` | `String` sin `length` | `Rol.descripcion` |
| `numeric(p,s)` | `BigDecimal` + `precision`/`scale` | `Vehiculo.precioBase` |
| `boolean` | `Boolean` | `Usuario.activo` |
| `timestamp with time zone` | `LocalDateTime` | `Venta.fechaCreacion` |
| `date` | `LocalDate` | `Cotizacion.vigenciaHasta` |
| `integer` | `Integer` | `Vehiculo.kilometraje` |
| `smallint` | `Short` | `Vehiculo.anio` |
| `jsonb` | `String` + `columnDefinition = "jsonb"` | `Auditoria.datosNuevos` |

**Por qué `BigDecimal` y no `double` para el dinero:** `double` es binario y no puede
representar exactamente valores como `0.1`; los redondeos se acumulan. `BigDecimal` es
decimal exacto, y es lo que corresponde a `numeric` de PostgreSQL.

---

## 5. Las relaciones

### `@ManyToOne`, el caso normal

La FK vive en la tabla del lado "muchos", así que la anotación va en esa clase:

```java
// en Vehiculo: muchos vehículos comparten un modelo
@ManyToOne
@JoinColumn(name = "modelo_id", nullable = false)
private Modelo modelo;
```

**Solo se mapea este lado.** No hay un `@OneToMany` inverso (`Modelo.vehiculos`). Ver §6.

### Varias FK a la misma tabla

Pasa mucho con `usuarios`. Lo que las distingue es el `name` del `@JoinColumn`:

```java
// en OportunidadVenta
@ManyToOne
@JoinColumn(name = "comprador_id", nullable = false)
private Usuario comprador;

@ManyToOne
@JoinColumn(name = "asesor_id")          // sin nullable = false: es opcional
private Usuario asesor;
```

### FK con UNIQUE = relación 1 a 1

Cuando la FK además es única, la relación es uno a uno. Se sigue mapeando con
`@ManyToOne` más `unique = true`:

```java
// en Venta: cada oportunidad produce como máximo una venta
@ManyToOne
@JoinColumn(name = "oportunidad_id", nullable = false, unique = true)
private OportunidadVenta oportunidad;
```

### `@ManyToMany`, las dos tablas puente

`usuarios_roles` y `roles_permisos` solo tienen dos FK y ninguna columna propia. No
merecen una clase: se declaran como colección en el lado que las usa.

```java
// en Usuario
@ManyToMany
@JoinTable(name = "usuarios_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),        // apunta a ESTA clase
        inverseJoinColumns = @JoinColumn(name = "rol_id"))     // apunta a la OTRA
private List<Rol> roles;
```

En `Rol` va lo mismo contra `roles_permisos` hacia `Permiso`.

---

## 6. Lo que se decidió NO usar, y por qué

Esta es la sección que más sirve cuando preguntan "¿y por qué no hiciste...?".

**`enum` de Java con `@Enumerated(EnumType.STRING)`.**
Las columnas de estado son `varchar` con un `CHECK` en la BD (`vehiculos.estado` solo
acepta `INGRESADO`, `PUBLICADO`, `VENDIDO`...). Se mapean como `String`. Un `enum` daría
validación en tiempo de compilación, pero son unos 20 archivos nuevos y la restricción ya
está garantizada donde importa: en la base de datos.

**`@OneToMany` inverso (relaciones bidireccionales).**
Solo se mapea el lado dueño. Razón práctica: con Lombok `@Data`, una relación
bidireccional hace que el `toString()` de A llame al de B, que llama al de A, y se va en
recursión infinita. Además, sin el inverso no hay riesgo de cargar media base de datos
sin querer.

**`fetch = FetchType.LAZY`.**
Se deja el valor por defecto de JPA, que para `@ManyToOne` es `EAGER`. En un modelo
grande `LAZY` suele ser mejor, pero eso se ajusta cuando existan repositorios y consultas
reales, no antes.

**`@Table(uniqueConstraints = @UniqueConstraint(...))`.**
Dos tablas tienen `UNIQUE` sobre dos columnas juntas: `modelos(marca_id, nombre)` y
`permisos(modulo, accion)`. El `unique = true` de `@Column` solo sirve para una columna,
así que estas dos restricciones no se declaran en el código. Siguen activas en
PostgreSQL, que es quien las hace cumplir.

**Una librería para `jsonb`.**
Mapear `jsonb` a un objeto Java real pediría `hypersistence-utils` (dependencia externa)
o `@JdbcTypeCode(SqlTypes.JSON)`. Se mapea como `String` con
`columnDefinition = "jsonb"`: cero dependencias nuevas, y el JSON se maneja como texto.

**`ddl-auto`.**
No está configurado, así que Hibernate **no toca el esquema**. La base de datos se creó
con SQL a mano y manda ella; las entidades se limitan a reflejarla.

---

## 7. Las últimas 5 entidades: cierre de venta y postventa

Ninguna otra tabla las referencia con FK, así que el proyecto compilaba sin ellas
mientras estaban vacías. Ya están mapeadas.

| Clase | Tabla | Relación con `ventas` | Qué tiene de particular |
|---|---|---|---|
| `Factura` | `facturas` | 1 a 1 (`venta_id UNIQUE`) | Tres `numeric(15,2)` (`subtotal`, `iva`, `total`). |
| `Contrato` | `contratos` | 1 a 1 (`venta_id UNIQUE`) | Dos `boolean` de firma. Fechas opcionales y obligatorias mezcladas. |
| `Garantia` | `garantias` | 1 a 1 (`venta_id UNIQUE`) | Dos columnas `date` (`LocalDate`) y un `smallint` (`Short`). Columnas `text`. |
| `Pqrs` | `pqrs` | N a 1, **opcional** (`venta_id` sin `UNIQUE`, sin `NOT NULL`) | Tres FK en total: `comprador_id` (obligatoria), `usuario_asignado_id` (opcional), ambas a `usuarios` y distinguidas por el `name` del `@JoinColumn`. |
| `Notificacion` | `notificaciones` | no aplica, FK es a `usuarios` | `usuario_id` con `ON DELETE CASCADE` (se maneja en la BD, no en la anotación). Mezcla `text` y `varchar`. `referencia_id` es un `Long` suelto, sin FK real: es una columna polimórfica genérica. |

`Factura`, `Contrato` y `Garantia` son las tres que se generan **una vez por venta** al
cerrarla, por eso su FK a `ventas` lleva `UNIQUE`. `Pqrs` y `Notificacion` se acumulan
con el tiempo (un comprador puede radicar varias PQRS, un usuario recibe muchas
notificaciones), por eso ninguna de sus FK es única.

Para ver las columnas de una tabla, en `psql`:

```sql
\d facturas
```

O desde la ventana Database de IntelliJ, abriendo la tabla. También están todas en
`Semestre_6/Sistemas computacioanales basados en Servicios/Semana 2/database/schema.sql`.

---

## 8. Preguntas probables

**¿Por qué `IDENTITY` y no `SEQUENCE` o `AUTO`?**
Porque las PK son `BIGSERIAL`: PostgreSQL ya tiene una secuencia con un
`DEFAULT nextval(...)` en la columna. `IDENTITY` le dice a Hibernate que deje que la BD
asigne el valor durante el `INSERT` y lo lea después.

**¿Por qué toda entidad necesita `@NoArgsConstructor`?**
Hibernate instancia la entidad por reflexión, sin argumentos, y luego llena los campos.
Si la clase solo tuviera el constructor con todos los parámetros (el que genera
`@AllArgsConstructor`), Hibernate no podría crearla y fallaría al arrancar.

**¿Por qué el `@JoinColumn` va en el lado del `@ManyToOne`?**
Porque ahí es donde está físicamente la columna FK. En `vehiculos` existe la columna
`modelo_id`; en `modelos` no hay ninguna columna que apunte a vehículos.

**¿Qué pasa si el `nullable = false` del código no coincide con la BD?**
Como `ddl-auto` no está activo, Hibernate no cambia nada. Simplemente el `INSERT` fallará
contra la restricción real de PostgreSQL. Por eso las anotaciones se copiaron del esquema
real, y no al revés.

**¿Por qué `Marca` y `EntidadFinanciera` no tienen relaciones?**
Son tablas catálogo: otras tablas las referencian a ellas, no al contrario. Como no se
mapea el lado inverso (§6), no les corresponde ningún atributo de relación.

**¿Cómo compruebo que el mapeo está bien?**

```powershell
$env:DB_PASSWORD = "tu_password"
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=validate"
```

`validate` hace que Hibernate compare cada entidad contra el esquema real al arrancar y
falle diciendo qué columna no cuadra. Solo lee: nunca modifica la base de datos.
