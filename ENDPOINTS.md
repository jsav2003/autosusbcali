# Guía de endpoints REST — autosusbcali

Material de repaso de los controladores: **qué URL escribir, qué responde y por qué**.
Complementa a `MAPEO.md`, que cubre las entidades.

La app corre en `http://localhost:8080`. Todas las rutas empiezan por `/api`.

---

## 1. Endpoints disponibles

### Roles — `RolController` (`/api/roles`)

| Método | URL | Qué devuelve | Si no existe |
|---|---|---|---|
| GET | `/api/roles` | todos los roles | — (lista vacía) |
| GET | `/api/roles/activos` | los roles con `activo = true` | — (lista vacía) |
| GET | `/api/roles/inactivos` | los roles con `activo = false` | — (lista vacía) |
| GET | `/api/roles/por-id/{id}` | un rol, buscado por id | 404 |
| GET | `/api/roles/por-nombre/{nombre}` | un rol, buscado por nombre | 404 |

Todos responden con `ObtenerRolResponse` (`id` y `nombre`), no con la entidad: ver §5.

Ejemplos para copiar tal cual:

```
http://localhost:8080/api/roles
http://localhost:8080/api/roles/activos
http://localhost:8080/api/roles/por-id/1
http://localhost:8080/api/roles/por-nombre/ADMIN
```

#### Eliminar un rol — `DELETE /api/roles/eliminar/{id}`

**Borrado lógico:** el rol no se borra de la base de datos, solo se pone `activo = false`.
Por eso después aparece en `/api/roles/inactivos` y deja de aparecer en `/activos`.
Si se borrara la fila de verdad, se perderían las relaciones que la apuntan.

| Situación | Respuesta |
|---|---|
| el rol existe y está activo | **204** No Content (sin cuerpo); queda inactivo |
| el id no existe | **404** "Rol no encontrado" |
| el rol ya estaba inactivo | **500** "El rol ya se encuentra inactivo" |

```
DELETE http://localhost:8080/api/roles/eliminar/5
```

El navegador solo hace GET, así que este endpoint se prueba con Postman o `curl`:

```
curl -X DELETE http://localhost:8080/api/roles/eliminar/5
```

Probado el 02/11 con la app corriendo (ver las tres respuestas de la tabla).

Datos que hay en `servicios_db` para probar:

| id | nombre |
|---|---|
| 1 | `ADMIN` |
| 2 | `ASESOR` |
| 3 | `GERENTE_VENTAS` |
| 4 | `COMPRADOR` |

---

## 2. Cómo leer una URL con `{llaves}`

En la documentación, `{id}` o `{nombre}` es un **hueco que se rellena**, no algo que se
escribe literal:

| Documentación | Lo que se escribe en el navegador |
|---|---|
| `/api/roles/por-id/{id}` | `/api/roles/por-id/1` |
| `/api/roles/por-nombre/{nombre}` | `/api/roles/por-nombre/ADMIN` |

**Error típico:** pegar `/por-nombre/{{nombre}}`. Las llaves dobles son la sintaxis de
variables de **Postman**. Si la variable no está creada en un environment activo,
Postman la manda literal y el servidor responde **400**, porque `{` y `}` no son válidos
en una URL.

---

## 3. Qué significa cada respuesta

| Código | Cuándo sale aquí | Ejemplo |
|---|---|---|
| **200** OK | se encontró | `/por-id/1` |
| **204** No Content | se hizo la acción y no hay nada que devolver | `DELETE /eliminar/5` |
| **404** Not Found | la búsqueda no encontró nada | `/por-id/99`, `/por-nombre/admin` |
| **500** Internal Server Error | una validación del servicio lanzó `Exception` | `DELETE /eliminar/5` sobre un rol ya inactivo |
| **400** Bad Request | la URL no se puede convertir al tipo del parámetro | `/por-id/abc` (no es número), `/por-nombre/{{nombre}}` |

- **La búsqueda por nombre distingue mayúsculas.** En la BD está `ADMIN`, así que `admin`
  da 404. Para ignorarlas: `findByNombreIgnoreCase` en el repositorio.
- **El 400 de `/por-id/abc` lo lanza Spring, no nuestro código.** `id` es `Long`, y
  `"abc"` no se puede convertir, así que el método nunca llega a ejecutarse.

---

## 4. Cómo está escrito un endpoint

```java
@GetMapping("/por-id/{id}")                                                // (1)
public ResponseEntity<ObtenerRolResponse> obtenerPorId(@PathVariable Long id) { // (2)
    Optional<Rol> rolOptional = rolRepository.findById(id);               // (3)
    if (rolOptional.isPresent()) {
        return new ResponseEntity<>(
                RolMapper.rolAObtenerRolResponse(rolOptional.get()), HttpStatus.OK); // (4)
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);                    // (5)
}
```

1. `@GetMapping("/por-id/{id}")` — responde a GET en esa ruta. Se suma a la ruta de la
   clase (`@RequestMapping("api/roles")`). `{id}` marca el trozo variable.
2. `@PathVariable Long id` — toma el `{id}` de la URL y lo convierte a `Long`. El nombre
   del parámetro tiene que coincidir con el de las llaves.
3. `findById` viene gratis de `JpaRepository`. Devuelve `Optional` porque puede no haber
   nada con ese id. Aquí todavía se trabaja con la **entidad** `Rol`.
4. Si hay rol → se convierte a DTO con el mapper (§5) y se responde **200** con él en el
   cuerpo (Spring lo convierte a JSON).
5. Si no → **404** sin cuerpo. El mapper no se llama: no hay nada que convertir.

**Por qué `ResponseEntity` y no devolver el objeto directo:** `ResponseEntity` deja elegir
el código HTTP. Si el método devolviera el rol y no lo encontrara, respondería 200 vacío,
y el cliente no sabría que el rol no existe.

**Las listas no llevan `ResponseEntity`:** `obtenerTodos()` devuelve
`List<ObtenerRolResponse>` sin más. Una lista vacía es una respuesta válida (200 con
`[]`), no un error.

### Forma equivalente, más corta

```java
return rolRepository.findById(id)
        .map(RolMapper::rolAObtenerRolResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
```

Hace exactamente lo mismo que el `if`. Usar la que se entienda mejor.

---

## 5. DTO y mapper: no se devuelve la entidad

Los endpoints **no devuelven `Rol`** (la entidad JPA), sino `ObtenerRolResponse`, un DTO
con solo lo que el cliente necesita:

```java
public record ObtenerRolResponse(Long id, String nombre) { }
```

La conversión la hace `RolMapper` (paquete `mapper`), con dos métodos estáticos:

| Método | Convierte |
|---|---|
| `rolAObtenerRolResponse(Rol)` | un rol → un DTO |
| `listaRolesAlistaObtenerRolResponse(List<Rol>)` | una lista → lista de DTO (`stream().map(...).toList()`) |

Respuesta de `GET /api/roles`:

```json
[{"id":1,"nombre":"ADMIN"},{"id":2,"nombre":"ASESOR"}, ...]
```

**Por qué no devolver la entidad:**

- **Se expone de más.** `Rol` trae `descripcion`, `activo` y la lista entera de
  `permisos` (por el `@ManyToMany`, `EAGER`). Con la entidad, un solo rol ocupaba cientos
  de caracteres; con el DTO, dos campos.
- **La API queda atada a la BD.** Si mañana cambia una columna, cambia el JSON que reciben
  los clientes. Con DTO, el contrato de la API se decide aparte.
- **Riesgo de recursión.** Entidades con relaciones en los dos sentidos pueden generar un
  JSON infinito al serializarse. Un DTO plano no tiene ese problema.

**Por qué `record`:** es una clase inmutable de solo datos; Java genera el constructor,
los accesores (`id()`, `nombre()`), `equals`, `hashCode` y `toString`. No hace falta
Lombok.

**Flujo completo:** repositorio → `Rol` (entidad) → `RolMapper` → `ObtenerRolResponse`
(DTO) → JSON.

---

## 6. De dónde sale cada método del repositorio

| Método | Origen |
|---|---|
| `findAll()`, `findById(id)` | heredados de `JpaRepository<Rol, Long>`: no se escriben |
| `findByNombre(String nombre)` | escrito en `RolRepository`; Spring arma la consulta |
| `findByActivo(Boolean activo)` | ídem |

Spring **lee el nombre del método** y genera la consulta: `findBy` + nombre del campo de
la entidad (`nombre`, `activo`). Si el campo no existe en `Rol`, la app **no arranca**.
El nombre del parámetro da igual; el del método, no.

Tipo de retorno:

- `Optional<Rol>` cuando hay como mucho uno (`nombre` es `unique` en la entidad).
- `List<Rol>` cuando puede haber varios (`activo`).

---

## 7. Errores que ya salieron (y qué significaban)

| Mensaje | Causa real |
|---|---|
| `compact source file should not have package declaration` | métodos escritos **fuera** de la `}` que cierra la clase |
| `';' expected` + `not a statement` en la misma línea | paréntesis de más: `new ResponseEntity<>(x.get()),HttpStatus.OK)` |
| import `javax.swing.text.html.Option` | el autocompletado eligió `Option` (Swing) en vez de `Optional` (`java.util`) |
| dos métodos llamados `obtenerPorNombre` | compila (sobrecarga por tipo de parámetro), pero confunde; se renombró a `obtenerPorId` |
| la app sigue respondiendo el rol completo tras cambiar el código | la app no se reinició: corre la versión compilada anterior |
