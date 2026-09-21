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

Ejemplos para copiar tal cual:

```
http://localhost:8080/api/roles
http://localhost:8080/api/roles/activos
http://localhost:8080/api/roles/por-id/1
http://localhost:8080/api/roles/por-nombre/ADMIN
```

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
| **404** Not Found | la búsqueda no encontró nada | `/por-id/99`, `/por-nombre/admin` |
| **400** Bad Request | la URL no se puede convertir al tipo del parámetro | `/por-id/abc` (no es número), `/por-nombre/{{nombre}}` |

- **La búsqueda por nombre distingue mayúsculas.** En la BD está `ADMIN`, así que `admin`
  da 404. Para ignorarlas: `findByNombreIgnoreCase` en el repositorio.
- **El 400 de `/por-id/abc` lo lanza Spring, no nuestro código.** `id` es `Long`, y
  `"abc"` no se puede convertir, así que el método nunca llega a ejecutarse.

---

## 4. Cómo está escrito un endpoint

```java
@GetMapping("/por-id/{id}")                                   // (1)
public ResponseEntity<Rol> obtenerPorId(@PathVariable Long id) { // (2)
    Optional<Rol> rolOptional = rolRepository.findById(id);  // (3)
    if (rolOptional.isPresent()) {
        return new ResponseEntity<>(rolOptional.get(), HttpStatus.OK); // (4)
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);       // (5)
}
```

1. `@GetMapping("/por-id/{id}")` — responde a GET en esa ruta. Se suma a la ruta de la
   clase (`@RequestMapping("api/roles")`). `{id}` marca el trozo variable.
2. `@PathVariable Long id` — toma el `{id}` de la URL y lo convierte a `Long`. El nombre
   del parámetro tiene que coincidir con el de las llaves.
3. `findById` viene gratis de `JpaRepository`. Devuelve `Optional` porque puede no haber
   nada con ese id.
4. Si hay rol → **200** con el rol en el cuerpo (Spring lo convierte a JSON).
5. Si no → **404** sin cuerpo.

**Por qué `ResponseEntity` y no devolver `Rol` directo:** `ResponseEntity` deja elegir el
código HTTP. Si el método devolviera `Rol` y no lo encontrara, respondería 200 vacío, y
el cliente no sabría que el rol no existe.

**Las listas no llevan `ResponseEntity`:** `obtenerTodos()` devuelve `List<Rol>` sin más.
Una lista vacía es una respuesta válida (200 con `[]`), no un error.

### Forma equivalente, más corta

```java
return rolRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
```

Hace exactamente lo mismo que el `if`. Usar la que se entienda mejor.

---

## 5. De dónde sale cada método del repositorio

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

## 6. Errores que ya salieron (y qué significaban)

| Mensaje | Causa real |
|---|---|
| `compact source file should not have package declaration` | métodos escritos **fuera** de la `}` que cierra la clase |
| `';' expected` + `not a statement` en la misma línea | paréntesis de más: `new ResponseEntity<>(x.get()),HttpStatus.OK)` |
| import `javax.swing.text.html.Option` | el autocompletado eligió `Option` (Swing) en vez de `Optional` (`java.util`) |
| dos métodos llamados `obtenerPorNombre` | compila (sobrecarga por tipo de parámetro), pero confunde; se renombró a `obtenerPorId` |
