# back-SGRA

Backend del Sistema de Gestión de Refuerzos Académicos (SGRA), desarrollado con **Spring Boot 4** y **Java 21**.

---

## ⚙️ Requisitos previos

Antes de ejecutar el proyecto asegúrate de tener instalado:

| Herramienta | Versión mínima | Descarga |
|---|---|---|
| Java JDK | 21 | https://learn.microsoft.com/es-es/java/openjdk/download |
| Maven | 3.9+ | (incluido via `mvnw`) |
| Azure CLI | Última | https://aka.ms/installazurecliwindows |

---

## 🔑 Configuración de Azure CLI (obligatorio en desarrollo local)

Los secretos del proyecto (contraseña de BD, API keys, etc.) están almacenados en **Azure Key Vault** (`kv-sgra`). El proyecto usa `DefaultAzureCredential` que se autentica automáticamente según el entorno:

| Entorno | Autenticación | Requiere |
|---|---|---|
| **Desarrollo local** | Azure CLI | `az login` con tu cuenta |
| **Producción (Azure)** | Managed Identity | Activarla en el recurso de Azure — sin contraseñas ni CLI |

> En producción no se necesita ningún cambio en el código ni en `application.properties`. Azure se autentica solo a través de la Managed Identity del recurso donde corre la app.

**1. Instalar Azure CLI** (si no lo tienes):
```
winget install Microsoft.AzureCLI
```

**2. Iniciar sesión con tu cuenta institucional:**
```
az login
```
Se abrirá el navegador. Usa tu cuenta `@msuteq.edu.ec`.

**3. Verificar que tienes acceso al Key Vault:**
```
az keyvault secret list --vault-name kv-sgra --query "[].name" -o tsv
```
Debes ver listados los secretos (`sgra-master-key`, `spring-datasource-password`, etc.). Si ves un error de permisos, pide al administrador que te agregue al Key Vault.

---

## 🚀 Configuración del proyecto

**1. Clonar el repositorio y entrar al directorio:**
```
cd back-SGRA
```

**2. Ejecutar el proyecto:**
```
mvnw spring-boot:run
```
O directamente desde IntelliJ IDEA con el botón ▶️.

> El archivo `application.properties` ya viene en el repo. No necesitas configurar nada adicional — los secretos se cargan automáticamente desde Azure Key Vault usando tu sesión de Azure CLI.

---

## 🧱 Inicialización única de BD (primer arranque)

Si necesitas ejecutar `schema.sql` completo solo una vez desde la app:

**1. Configura credenciales de bootstrap**
- En `application-bootstrap-init.properties` (`spring.sql.init.username/password`) o
- como secretos de Key Vault si decides externalizarlas.

**2. Ejecuta solo el primer arranque con perfil `bootstrap-init`:**
```
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=bootstrap-init"
```

Ese perfil desactiva `spring.sql.init` de Spring y usa `psql` para ejecutar `schema.sql`
antes de inicializar JPA. Si ya existe la tabla marcador (`seguridad.tbroles`), no lo vuelve a ejecutar.

> Si aparece `CreateProcess error=2` o `No se encontro el ejecutable 'psql'`, instala el cliente de PostgreSQL
> o configura ruta completa en `application-bootstrap-init.properties`:
> `bootstrap.psql.command=C:/Program Files/PostgreSQL/17/bin/psql.exe`

**3. En arranques normales, no uses ese perfil:**
```
mvnw spring-boot:run
```

Con eso, la app ya no vuelve a correr `schema.sql` en corridas posteriores.

---

## 🔐 Secretos en Azure Key Vault

El proyecto usa `spring-cloud-azure-starter-keyvault-secrets` para cargar los secretos automáticamente al arrancar. No necesitas copiar ningún valor manualmente.

| Nombre del secreto en Key Vault | Para qué se usa |
|---|---|
| `spring-datasource-password` | Contraseña de PostgreSQL |
| `sgra-master-key` | Llave maestra de encriptación interna |
| `groq-api-key` | API Key de Groq AI |
| `spring-cloud-azure-storage-blob-connection-string` | Connection string de Azure Blob Storage |

---

## 🛠️ Estructura principal

```
src/main/java/com/CLMTZ/Backend/
├── config/          # Configuraciones (CORS, Azure Storage, Groq, etc.)
├── controller/      # Endpoints REST
├── service/         # Lógica de negocio
├── repository/      # Acceso a datos (JPA + queries nativas)
├── model/           # Entidades JPA
└── dto/             # Objetos de transferencia de datos
```

---

## ❓ Problemas comunes

**WARNING: "A restricted method in java.lang.System has been called" (Netty)**
→ Es un warning inofensivo de Java 25 con Netty. Se suprime automáticamente con la Run Configuration incluida en el repo (`--enable-native-access=ALL-UNNAMED`). Si persiste, ve a IntelliJ → Run → Edit Configurations → VM options y agrega `--enable-native-access=ALL-UNNAMED`.

**"Could not resolve placeholder 'sgra-master-key'"**
→ No estás autenticado en Azure CLI. Ejecuta `az login`.

**"No qualifying bean of type BlobServiceClient"**
→ El secreto `spring-cloud-azure-storage-blob-connection-string` no se cargó del Key Vault. Verifica tu acceso con `az keyvault secret list --vault-name kv-sgra`.

**"Access denied" al Key Vault**
→ Tu cuenta no tiene permisos. Pide al administrador del proyecto que te asigne el rol `Key Vault Secrets User` en el recurso `kv-sgra`.
