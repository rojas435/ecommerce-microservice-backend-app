# CI / Jenkins — guía CLARA y paso a paso

Esta guía te dice exactamente qué hacer para ejecutar los pipelines Dev (construcción y pruebas unitarias) de `order-service`, `payment-service` y `shipping-service`.

Archivos que ya existen en el repo
- `order-service/Jenkinsfile`
- `payment-service/Jenkinsfile`
- `shipping-service/Jenkinsfile`

¿Qué hace cada Jenkinsfile?
- Corre en un agente Jenkins.
- Ejecuta: `mvn clean package` (incluye pruebas unitarias).
- Publica: artefactos `.jar` y reportes JUnit.
- Opcional: construye y publica imagen Docker si activas `PUBLISH=true` y configuras credenciales.

IMPORTANTE: estos son pipelines Dev (build + unit tests). No despliegan ni corren E2E. Eso lo haremos más adelante en pipelines Stage/Master.

## PASO 0 — Asegúrate de que el repo tiene los Jenkinsfile (y haz push)
En tu terminal de desarrollo, desde la raíz del proyecto:
```powershell
git status
git add order-service/Jenkinsfile payment-service/Jenkinsfile shipping-service/Jenkinsfile ci/jenkins/README.md
git commit -m "ci: add Dev Jenkinsfiles and CI readme"
git push origin master
```

## PASO 1 — Pre-requisitos en Jenkins (una sola vez)
1) Plugins mínimos:
  - Pipeline, Multibranch Pipeline, Git, Credentials, Credentials Binding, JUnit, Docker Pipeline.
2) Herramientas/Agente:
  - Agente con Maven y JDK disponibles en PATH.
  - Si vas a publicar imágenes: Docker CLI instalado y permisos para `docker`.
  - (Opcional) Agente etiquetado, por ejemplo `maven-docker`.
3) Jenkins > Manage Jenkins > Tools:
  - Configura Maven/JDK si usas herramientas administradas por Jenkins.

## PASO 2 — Crea credenciales (si vas a publicar imágenes)
Jenkins > Manage Jenkins > Credentials > (global):
- ID: `docker-registry-credentials`
  - Type: Username with password
  - Username/Password: los de tu registry

## PASO 3 — Crea un Multibranch Pipeline por servicio
Repite para cada servicio. Ejemplo para `order-service`:
1. New Item > nombre: `order-service-mbp` > tipo: Multibranch Pipeline > OK.
2. Branch Sources > Git (o GitHub) > URL del repo > añade credenciales si es privado.
3. Build Configuration > Script Path: pon `order-service/Jenkinsfile`.
4. Save. Jenkins escaneará ramas y creará jobs por rama.

Haz lo mismo para:
- `payment-service` con Script Path: `payment-service/Jenkinsfile`
- `shipping-service` con Script Path: `shipping-service/Jenkinsfile`

## PASO 4 — Ejecuta el pipeline (build + unit tests)
1. Entra al job multibranch (`order-service-mbp`).
2. Abre la rama `master` (o la que uses) y haz clic en "Build Now" / "Run".
3. Verifica la consola: debe ejecutar `mvn clean package` y publicar los JUnit.
4. Ve a "Artifacts" y comprueba el `.jar` en `target/`.

## PASO 5 — (Opcional) Habilita build/push de imagen Docker
En la configuración del job multibranch:
- Agrega variable `PUBLISH=true` (Environment variables).
- Define `DOCKER_REGISTRY`, por ejemplo: `registry.example.com/tu-org`.
- Asegúrate de haber creado la credencial `docker-registry-credentials`.
Vuelve a ejecutar: verás pasos de `docker build`, `docker login` y `docker push`.

## Dónde veo resultados
- Tests: pestaña JUnit del build (`**/target/surefire-reports/*.xml`).
- Artefactos: pestaña Artifacts (debe mostrar `target/*.jar`).
- Logs: consola del build (usa timestamps y colores para facilitar la lectura).

## Problemas comunes y solución rápida
- `mvn: command not found` → instala Maven en el agente o usa un contenedor/label con Maven.
- `java: not found` → instala JDK y configúralo.
- `docker: not found` → instala Docker en el agente y permite que Jenkins ejecute `docker`.
- Fallos de tests unitarios → abre `surefire-reports` y corrige el test o el código.

## Qué sigue (una vez que Dev pipelines funcionan)
1) Pipeline Stage: build + deploy a Kubernetes (namespace `stage`) + smoke/E2E.
2) Pipeline Master: build + validaciones + generación automática de Release Notes + despliegue con aprobación.
3) Añadir más pruebas: unitarias, integración (Testcontainers), E2E y Locust (performance).

Si quieres, en el próximo paso puedo añadir automáticamente:
- Un `Jenkinsfile` raíz (monorepo) para orquestar builds multi-servicio.
- Un `Jenkinsfile` de Stage con despliegue a Kubernetes y smoke tests.
- Un script de Release Notes y su integración en un Jenkinsfile Master.

## Pipeline agregador en la raíz (opcional)
Además de los Jenkinsfile por servicio, hay un `Jenkinsfile` en la raíz del repositorio que:
- Construye y ejecuta unit tests de `order-service`, `payment-service` y `shipping-service` en paralelo.
- Publica artefactos y reportes JUnit por módulo.
- Permite ejecutar E2E (`RUN_E2E=true`).

Cómo usarlo:
- Crea un job Pipeline o Multibranch apuntando al repo y deja `Script Path` como `Jenkinsfile` (por defecto).
- Requisitos del agente: Maven + JDK en PATH. Si corres en Windows agent, cambia `sh` por `bat` o usa un agente Linux.
