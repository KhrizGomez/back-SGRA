# API Docente — Módulo de Reforzamiento

Base URL: `/api/teacher`

---

## 1. Gestión de Solicitudes — `/api/teacher/requests`

### `GET /api/teacher/requests`
Lista las solicitudes de reforzamiento entrantes del docente autenticado.

**Query params:**
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `statusId` | `Integer` | No | Filtrar por ID de estado |
| `page` | `Integer` | No (default: 1) | Número de página |
| `size` | `Integer` | No (default: 10) | Tamaño de página (máx. 100) |

**Respuesta 200:**
```json
{
  "items": [
    {
      "requestId": 5,
      "studentName": "Juan Pérez",
      "subjectName": "Cálculo I",
      "sessionType": "Individual",
      "reason": "No entiendo integrales",
      "statusName": "Pendiente",
      "statusId": 1,
      "createdAt": "2026-03-01T10:30:00",
      "isGroupal": false,
      "participantCount": 1
    }
  ],
  "totalCount": 12,
  "page": 1,
  "size": 10,
  "totalPages": 2
}
```

---

### `PUT /api/teacher/requests/{requestId}/accept`
Acepta una solicitud pendiente y programa la sesión de reforzamiento.

**Path param:** `requestId` — ID de la solicitud.

**Body JSON:**
```json
{
  "scheduledDate": "2026-03-10",
  "timeSlotId": 3,
  "modalityId": 1,
  "estimatedDuration": "01:30",
  "reason": "Disponibilidad confirmada",
  "workAreaId": 2
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `scheduledDate` | `String` | Sí | Fecha programada (`YYYY-MM-DD`) |
| `timeSlotId` | `Integer` | Sí | ID de la franja horaria |
| `modalityId` | `Integer` | Sí | ID de la modalidad |
| `estimatedDuration` | `String` | Sí | Duración estimada (`HH:mm`) |
| `reason` | `String` | No | Motivo u observación |
| `workAreaId` | `Integer` | Condicional | Requerido si la modalidad es presencial |

**Respuesta 200:**
```json
{
  "id": 8,
  "status": "OK",
  "message": "Solicitud aceptada y sesión programada correctamente"
}
```

---

### `PUT /api/teacher/requests/{requestId}/reject`
Rechaza una solicitud pendiente.

**Path param:** `requestId` — ID de la solicitud.

**Body JSON (opcional):**
```json
{
  "reason": "No tengo disponibilidad en ese horario"
}
```

**Respuesta 200:**
```json
{
  "id": 5,
  "status": "OK",
  "message": "Solicitud rechazada correctamente"
}
```

---

### `PUT /api/teacher/requests/{requestId}/reschedule`
Reprograma una sesión ya aceptada. Solo disponible cuando el estado es `Aceptada`.

**Path param:** `requestId` — ID de la solicitud.

**Body JSON:** *(mismo esquema que `/accept`)*
```json
{
  "scheduledDate": "2026-03-15",
  "timeSlotId": 2,
  "modalityId": 1,
  "estimatedDuration": "01:00",
  "reason": "Se cambia por conflicto de horario",
  "workAreaId": 2
}
```

**Respuesta 200:**
```json
{
  "id": 8,
  "status": "OK",
  "message": "Sesión reprogramada correctamente"
}
```

---

### `PUT /api/teacher/requests/{scheduledId}/cancel`
Cancela una sesión aceptada o programada.

**Path param:** `scheduledId` — ID del refuerzo programado.

**Body JSON (opcional):**
```json
{
  "reason": "El docente no puede asistir"
}
```

**Respuesta 200:**
```json
{
  "id": 8,
  "status": "OK",
  "message": "Sesión cancelada correctamente"
}
```

---

## 2. Gestión de Sesiones — `/api/teacher/sessions`

### `PUT /api/teacher/sessions/{scheduledId}/virtual-link`
Registra o actualiza el enlace de reunión virtual para una sesión programada.

**Path param:** `scheduledId` — ID del refuerzo programado.

**Body JSON:**
```json
{
  "url": "https://meet.google.com/abc-defg-hij"
}
```

**Respuesta 200:**
```json
{
  "id": 8,
  "status": "OK",
  "message": "Enlace virtual registrado correctamente"
}
```

---

### `POST /api/teacher/sessions/{scheduledId}/attendance`
Registra la asistencia de cada participante en una sesión realizada.

**Path param:** `scheduledId` — ID del refuerzo programado.

**Body JSON:**
```json
{
  "performedId": 12,
  "attendances": [
    { "participantId": 3, "attended": true },
    { "participantId": 7, "attended": false }
  ]
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `performedId` | `Integer` | Sí | ID del refuerzo realizado |
| `attendances` | `Array` | Sí | Lista de asistencia (mínimo 1 elemento) |
| `attendances[].participantId` | `Integer` | Sí | ID del participante |
| `attendances[].attended` | `Boolean` | Sí | `true` si asistió, `false` si no |

**Respuesta 200:**
```json
{
  "id": 12,
  "status": "OK",
  "message": "Asistencia registrada correctamente"
}
```

---

### `POST /api/teacher/sessions/{scheduledId}/performed`
Registra el resultado de una sesión y sube archivos de recursos opcionales.

**Path param:** `scheduledId` — ID del refuerzo programado.

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `observation` | `String` | Sí | Observaciones finales de la sesión |
| `duration` | `String` | Sí | Duración real de la sesión (`HH:mm`) |
| `files[]` | `File[]` | No | Archivos de recursos (pueden ser varios) |

**Respuesta 200:**
```json
{
  "id": 12,
  "status": "OK",
  "message": "Resultado registrado correctamente"
}
```

---

## 3. Historial de Sesiones — `/api/teacher/history`

### `GET /api/teacher/history/sessions`
Devuelve el historial paginado de sesiones de reforzamiento del docente autenticado.

**Query params:**
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `page` | `Integer` | No (default: 1) | Número de página |
| `size` | `Integer` | No (default: 10) | Tamaño de página (máx. 100) |

**Respuesta 200:**
```json
{
  "items": [
    {
      "scheduledId": 8,
      "subjectName": "Cálculo I",
      "scheduledDate": "2026-03-10",
      "modality": "Virtual",
      "estimatedDuration": "01:30",
      "timeSlot": "08:00 - 10:00",
      "statusName": "Finalizada",
      "sessionType": "Individual",
      "studentCount": 1
    }
  ],
  "totalCount": 5,
  "page": 1,
  "size": 10,
  "totalPages": 1
}
```

---

## Respuestas de Error

Aplican a todos los endpoints.

**400 — Validación fallida:**
```json
{
  "message": "scheduledDate, timeSlotId, modalityId y estimatedDuration son requeridos"
}
```

**409 — Error de lógica de negocio:**
```json
{
  "id": null,
  "status": "ERROR",
  "message": "La solicitud no pertenece a este docente"
}
```

**500 — Error interno del servidor:**
```json
{
  "message": "Error aceptando solicitud: ..."
}
```
