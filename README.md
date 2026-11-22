# Turnos odontología

Monorepo de la fundación del sistema de turnos odontológicos.

## Backend

Requisitos: JDK 17+ y PostgreSQL solo para el perfil `local`.

PowerShell:

```powershell
Copy-Item .env.example .env
$env:SPRING_PROFILES_ACTIVE='local'
cd backend
./gradlew bootRun
```

Cargar manualmente las variables de PostgreSQL declaradas en `.env.example`. No se carga `.env` automáticamente: exportar las variables en la sesión antes de ejecutar el backend.

Perfil de pruebas:

```powershell
cd backend
./gradlew test
./gradlew build
```

Las pruebas usan `application-test.yml`, H2 en modo compatibilidad PostgreSQL y no requieren PostgreSQL instalado.

## Frontend

```powershell
cd frontend
npm.cmd install
npm.cmd run build
```

El resultado se escribe en `frontend/dist/`. En este change C-01, Spring Boot sirve los recursos estáticos mínimos desde `backend/src/main/resources/static/`.

No se guarda ninguna credencial real en el repositorio.
