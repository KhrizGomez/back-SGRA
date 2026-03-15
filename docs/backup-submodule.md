# Submódulo de Respaldos — SGRA

Base URL: `/api/admin/backup`

---

## Visión general

El submódulo de respaldos permite crear, programar y restaurar copias de seguridad completas de la base de datos PostgreSQL. Los archivos se almacenan en **Azure Blob Storage** (contenedor `backups-sgra`, prefijo `backups/`). Las operaciones de dump y restore se delegan a los binarios nativos de PostgreSQL (`pg_dump` / `pg_restore`) ejecutados como subprocesos del servidor.

```
┌────────────┐    POST /trigger      ┌─────────────────┐    pg_dump     ┌──────────────┐
│  Frontend  │ ─────────────────────▶│ BackupController │ ─────────────▶│  PostgreSQL  │
│  (Angular) │                       │  (Spring Boot)   │               │  (pg_dump)   │
│            │ ◀─────────────────────│                  │ ◀─────────────│              │
│            │    BackupResultDTO    └────────┬─────────┘               └──────────────┘
│            │                               │  sube .backup
│            │                               ▼
│            │                       ┌──────────────────┐
│            │    GET /history        │  Azure Blob      │
│            │ ─────────────────────▶│  Storage         │
│            │ ◀─────────────────────│  backups-sgra/   │
└────────────┘                       └──────────────────┘
```

---

## Clases principales

| Clase | Rol |
|---|---|
| `BackupController` | Expone los endpoints REST |
| `BackupServiceImpl` | Lógica de negocio: backup, restore, historial, schedules |
| `BackupScheduler` | Gestiona tareas cron dinámicas con `ThreadPoolTaskScheduler` |
| `BackupScheduleEntry` | Entidad JPA — tabla `general.tbprogramacionrespaldo` |

---

## Configuración requerida (`application.properties`)

```properties
backup.db-username=<usuario_postgres>
backup.db-password=<password_postgres>
backup.container-name=backups-sgra          # valor por defecto
backup.pgdump-path=                         # opcional: ruta explícita a pg_dump
```

El servicio también lee `spring.datasource.url` para extraer host, puerto y nombre de BD.

Si `backup.pgdump-path` está vacío, el servicio busca automáticamente el binario en rutas estándar de Windows (`C:/Program Files/PostgreSQL/{16-19}/bin/`) y Linux (`/usr/bin/`, `/usr/local/bin/`).

---

## 1. Backup Manual

### Endpoint
```
POST /api/admin/backup/trigger
```

### Flujo interno

```
1. Verifica sesión activa (UserContextHolder) → obtiene credenciales DB del usuario
2. Localiza pg_dump en el sistema
3. Genera nombre: sgra_YYYYMMDD_HHmmss_SSS.backup
4. Ejecuta pg_dump:
      pg_dump -h <host> -p <port> -U <user> -d <db> -F c -f <tempFile>
   • Formato custom (-F c): comprimido, compatible con pg_restore paralelo
   • Timeout: 10 minutos
5. Sube el archivo temporal a Azure Blob Storage (contenedor backups-sgra/backups/)
6. Elimina el archivo temporal
7. Retorna BackupResultDTO con nombre, URL, tamaño y timestamp
```

### Respuesta
```json
{
  "success": true,
  "message": "Respaldo completado exitosamente",
  "fileName": "sgra_20260315_143022_541.backup",
  "blobUrl": "https://<storage>.blob.core.windows.net/backups-sgra/backups/sgra_...",
  "fileSizeBytes": 204800,
  "executedBy": "admin@sgra.com",
  "executedAt": "15/03/2026 14:30"
}
```

---

## 2. Backup Automático (Programaciones)

### Arranque

Al iniciar la aplicación (`ApplicationReadyEvent`), `BackupServiceImpl` carga todas las entradas de `tbprogramacionrespaldo` y las registra en `BackupScheduler`.

### BackupScheduler

Mantiene un `ThreadPoolTaskScheduler` con pool de 4 hilos (`sgra-backup-*`). Cada programación se identifica por su `id` y se asocia a un `ScheduledFuture`. Al crear, actualizar o eliminar una programación desde la API, el scheduler cancela la tarea anterior y registra la nueva inmediatamente (sin reiniciar la app).

### Expresiones cron generadas

| Frecuencia | Ejemplo de cron |
|---|---|
| DIARIO | `0 0 2 * * *` (cada día a las 02:00) |
| SEMANAL | `0 30 3 * * MON` (cada lunes a las 03:30) |
| MENSUAL | `0 0 4 1 * *` (día 1 de cada mes a las 04:00) |

### Endpoints de programaciones

| Método | URL | Descripción |
|---|---|---|
| `GET` | `/api/admin/backup/schedules` | Lista todas las programaciones |
| `POST` | `/api/admin/backup/schedules` | Crea una nueva programación |
| `PUT` | `/api/admin/backup/schedules/{id}` | Actualiza frecuencia, hora o estado |
| `DELETE` | `/api/admin/backup/schedules/{id}` | Elimina la programación y cancela su tarea |

### Body de programación
```json
{
  "habilitado": true,
  "frecuencia": "SEMANAL",
  "diaSemana": "MON",
  "diaMes": null,
  "hora": 3,
  "minuto": 30
}
```

> `diaSemana` se usa solo con `SEMANAL` (valores: `SUN`, `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`).
> `diaMes` se usa solo con `MENSUAL` (valores: 1–28).

---

## 3. Historial y Descarga

### Listar backups
```
GET /api/admin/backup/history
```
Devuelve los blobs del contenedor ordenados por fecha de creación (más reciente primero).

```json
[
  {
    "fileName": "sgra_20260315_143022_541.backup",
    "blobUrl": "https://...",
    "fileSizeBytes": 204800,
    "createdAt": "15/03/2026 14:30"
  }
]
```

### URL de descarga temporal
```
GET /api/admin/backup/download/{fileName}
```
Genera una URL SAS de Azure con validez de **2 horas** para descarga directa sin exponer credenciales.

```json
{ "url": "https://<storage>.blob.core.windows.net/...?sv=...&sig=..." }
```

### Eliminar backup
```
DELETE /api/admin/backup/history/{fileName}
```

### Validar disponibilidad de pg_dump
```
GET /api/admin/backup/validate
```
```json
{ "available": true, "version": "pg_dump (PostgreSQL) 17.2" }
```

---

## 4. Restore

### Endpoint
```
POST /api/admin/backup/restore/{fileName}
```

### Flujo interno (optimizado)

```
1. Verifica existencia del blob en Azure
2. En PARALELO (CompletableFuture):
   ├── Descarga el .backup a un archivo temporal
   └── Renombra todos los schemas de usuario a {nombre}_bak
         → Permite rollback si falla el restore
3. Espera a que ambas operaciones terminen
4. Ejecuta pg_restore con paralelismo:
      pg_restore -h <host> -p <port> -U <user> -d <db>
                 --no-owner --disable-triggers
                 -j <N> -F c <tempFile>
   • -j N: workers paralelos (hasta 4, según CPUs disponibles)
   • --disable-triggers: desactiva FK checks durante la carga
   • Sin --single-transaction: permite el paralelismo
   • Timeout: 30 minutos
5a. Si pg_restore falla → rollbackSchemas():
      · Elimina schemas parciales del restore fallido
      · Renombra _bak de vuelta a sus nombres originales
5b. Si pg_restore tiene éxito → eliminarSchemasBak():
      · Elimina los schemas _bak
6. Invalida caché de Hibernate (entityManagerFactory.getCache().evictAll())
7. Soft-evict del pool de conexiones HikariCP
8. Elimina el archivo temporal
9. Retorna BackupResultDTO
```

### Diagrama de estados del restore

```
             ┌─────────────────────────────────────┐
             │  Descarga Azure  +  Rename _bak      │  (paralelo)
             └───────────┬─────────────────────────┘
                         │ ambos OK
                         ▼
                  ┌─────────────┐
                  │ pg_restore  │
                  │  -j N cores │
                  └──────┬──────┘
           ┌─────────────┴──────────────┐
        exitCode=0                  exitCode≠0
           │                            │
           ▼                            ▼
  eliminarSchemasBak()          rollbackSchemas()
  Evict Hibernate cache         (DB queda como estaba)
  Soft-evict HikariCP
           │
           ▼
     200 OK { success: true }
```

### Rollback automático

La estrategia de **rename** (en lugar de DROP directo) garantiza que si el restore falla a mitad de camino, la base de datos vuelve automáticamente al estado anterior:

```sql
-- Antes del restore
ALTER SCHEMA academico  RENAME TO academico_bak;
ALTER SCHEMA general    RENAME TO general_bak;
-- ... demás schemas

-- Si falla:
DROP SCHEMA academico CASCADE;           -- elimina lo parcial
ALTER SCHEMA academico_bak RENAME TO academico;  -- restaura original

-- Si tiene éxito:
DROP SCHEMA academico_bak CASCADE;       -- limpia _bak
```

### Respuesta
```json
{
  "success": true,
  "message": "Base de datos restaurada exitosamente desde sgra_20260315_143022_541.backup",
  "fileName": "sgra_20260315_143022_541.backup",
  "executedBy": "admin@sgra.com",
  "executedAt": "15/03/2026 15:00"
}
```

---

## 5. Tabla de la base de datos

**Schema:** `general`
**Tabla:** `tbprogramacionrespaldo`

| Columna | Tipo | Descripción |
|---|---|---|
| `idprogramacionrespaldo` | `INTEGER` PK | ID autoincremental |
| `idusuario` | `INTEGER` FK | Usuario que creó la programación |
| `habilitado` | `BOOLEAN` | Si la tarea cron está activa |
| `frecuencia` | `VARCHAR(10)` | `DIARIO`, `SEMANAL` o `MENSUAL` |
| `dia_semana` | `VARCHAR(10)` | Solo SEMANAL: `SUN`…`SAT` |
| `dia_mes` | `INTEGER` | Solo MENSUAL: 1–28 |
| `hora` | `INTEGER` | Hora de ejecución (0–23) |
| `minuto` | `INTEGER` | Minuto de ejecución (0–59) |
| `fecha_ultima_ejecucion` | `TIMESTAMP` | Cuándo corrió por última vez |
| `resultado_ultima_ejecucion` | `VARCHAR(500)` | `OK: <fileName>` o `ERROR: <msg>` |
| `fecha_creacion` | `TIMESTAMP` | Cuándo se creó el registro |

---

## Consideraciones de seguridad

- El usuario de backup (`backup.db-username`) debe tener permisos de **superuser** en PostgreSQL para que `--disable-triggers` funcione correctamente durante el restore.
- La contraseña se pasa al proceso hijo mediante la variable de entorno `PGPASSWORD` (nunca en argumentos de línea de comandos).
- Las URLs de descarga de Azure son SAS tokens temporales (2 horas) — nunca se expone la URL directa con credenciales permanentes.
- La conexión a PostgreSQL requiere `PGSSLMODE=require`.
