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

El modelo se está construyendo entidad por entidad. Hasta ahora:

- **Implementadas:** `Marca`, `EntidadFinanciera`, `SolicitudCredito`
- **Pendientes (clases creadas, aún vacías):** `Auditoria`, `Contrato`, `Cotizacion`,
  `Factura`, `Garantia`, `Modelo`, `Negociacion`, `Notificacion`, `Pago`, `Permiso`,
  `Pqrs`, `Reserva`, `Rol`, `Usuario`, `Vehiculo`, `Venta`

Todavía no hay repositorios, servicios ni controladores REST.
