# autosusbcali

Aplicación Spring Boot para la gestión de un concesionario de vehículos.
Proyecto de la asignatura **Sistemas Computacionales Basados en Servicios** — Universidad de San Buenaventura Cali, semestre 6.

## Stack

| | |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL |
| Utilidades | Lombok, Spring Boot DevTools |
| Build | Maven (con wrapper `mvnw`) |

## Requisitos

- JDK 25
- PostgreSQL en ejecución con una base de datos creada (por defecto `servicios_db`)

## Configuración

La conexión a la base de datos se lee de variables de entorno, con valores por
defecto para todo excepto la contraseña, que es obligatoria:

| Variable | Por defecto |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `servicios_db` |
| `DB_USERNAME` | `servicios_user` |
| `DB_PASSWORD` | *(sin valor por defecto — hay que definirla)* |

En PowerShell:

```powershell
$env:DB_PASSWORD = "tu_password"
```

## Cómo ejecutarlo

```powershell
.\mvnw spring-boot:run
```

Otros comandos útiles:

```powershell
.\mvnw clean compile   # compilar
.\mvnw test            # correr las pruebas
.\mvnw clean package   # generar el .jar en target/
```

## Estructura

```
src/main/java/co/edu/usbcali/autosusbcali/
├── AutosusbcaliApplication.java   punto de entrada
└── domain/                        entidades JPA
```

## Estado del modelo de dominio

La base de datos tiene **33 tablas** y el modelo **31 clases**, todas mapeadas:
`usuarios_roles` y `roles_permisos` son tablas puente de dos FK y se resuelven con
`@ManyToMany` dentro de `Usuario` y `Rol`, sin clase propia.

Catálogos y seguridad: `Usuario`, `Rol`, `Permiso`. Inventario: `Marca`, `Modelo`,
`Vehiculo`, `FichaTecnica`, `ImagenVehiculo`, `DocumentoVehiculo`, `VerificacionRunt`.
Proceso comercial: `OportunidadVenta`, `Negociacion`, `Reserva`, `CitaServicio`,
`Cotizacion`, `AccesorioCotizacion`. Crédito: `EntidadFinanciera`, `SolicitudCredito`.
Cierre y postventa: `Venta`, `Pago`, `TransaccionPasarela`, `TramiteTraspaso`, `Factura`,
`Contrato`, `Garantia`, `Pqrs`, `Notificacion`. Soporte: `Auditoria`,
`TokenRecuperacion`, `BusquedaGuardada`, `ParametroSistema`.

Todavía no hay repositorios, servicios ni controladores REST.

Las convenciones de mapeo, los tipos usados y las decisiones de diseño están en
[MAPEO.md](MAPEO.md).
