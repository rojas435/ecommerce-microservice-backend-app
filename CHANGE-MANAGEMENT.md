# Change Management & Release Notes Process

## 1. Objectives
Asegurar que cada cambio en el repositorio siga un proceso formal, trazable y reversible. Este documento cubre:
- Flujo de ramas y versionamiento.
- Criterios para preparar y aprobar releases.
- Generación automática de Release Notes.
- Etiquetado estandarizado de versiones.
- Planes de rollback ante fallos en producción.

## 2. Branching Strategy
| Branch | Propósito | Reglas |
|--------|-----------|--------|
| `master` | Código estable en producción | Solo merges desde `develop` o hotfix; protegido. |
| `develop` | Integración continua para próxima release | Features se integran vía PR. |
| `feature/*` | Desarrollo de nuevas funcionalidades | Se crean desde `develop`; nombre descriptivo. |
| `hotfix/*` | Correcciones urgentes sobre producción | Se crean desde `master`; merge a `master` y `develop`. |
| `release/*` (opcional) | Congelar versión para QA | Se crea desde `develop`, luego merge a `master` y `develop`. |

## 3. Commit & Conventional Messages
Se recomienda formato (similar a Conventional Commits) para permitir mejores notas:
```
<type>(<scope>): <short summary>

Optional body

BREAKING CHANGE: details (si aplica)
```
Tipos sugeridos: `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`.

## 4. Versioning
- Se utiliza SemVer: `MAJOR.MINOR.PATCH`.
- El pipeline invoca GitVersion. Si GitVersion no provee versión, se usa fallback `YYYY.MM.DD.<run>`.
- Tags: `v<semver>` (ej: `v1.4.2`). Nunca se publica un tag vacío.

## 5. Release Flow
1. Merge estable a `master`.
2. Pipeline ejecuta stage de semantic version.
3. Genera `RELEASE_NOTES.md` con commits desde último tag.
4. Crea tag `vX.Y.Z` y publica GitHub Release con notas.
5. Despliegue automático a entorno objetivo (dev/stage/prod según triggers/approval).

## 6. Automatic Release Notes
Script de GitHub Actions:
- Detecta último tag (`git tag --sort=-creatordate | head -n 2`).
- Lista commits entre ese tag y `HEAD` excluyendo merges.
- Formatea tabla de cambios agrupando por tipo.

## 7. Rollback Plan
### 7.1 Aplicación (Kubernetes / AKS)
- Cada despliegue usa imágenes versionadas en ACR: `<service>:<semver>` y `<service>:<short_sha>`.
- Rollback rápido: `kubectl set image deployment/<service> <container>=ACR/service:<previous-version>` o `kubectl rollout undo deployment/<service> -n <namespace>` si se mantiene historial.
- Para todos los microservicios:
```
for d in api-gateway order-service payment-service shipping-service product-service user-service; do 
  kubectl rollout undo deployment/$d -n ecommerce-minimal;
done
```

### 7.2 Datos
- Si hay cambios de esquema, incluir migraciones reversibles (Liquibase/Flyway) con scripts `down` antes de despliegue.
- Mantener respaldo de DB antes de migraciones (snapshot / dump).

### 7.3 Procedimiento Formal
1. Detectar incidencia crítica.
2. Confirmar versión afectada (Release Notes, tag, imagen).
3. Ejecutar rollback de despliegues a último estado estable.
4. Restaurar DB si es necesario.
5. Registrar acción en Issue con etiqueta `rollback`.

## 8. Change Approval
- PRs requieren: revisión + CI verde + cobertura mínima (definir threshold) + security scan sin CVE críticas.
- Para producción: aprobación manual del Environment en GitHub + confirmación de checklist (logs limpios, métricas OK).

## 9. Artefactos & Trazabilidad
| Artefacto | Generado por | Retención |
|-----------|--------------|-----------|
| JARs | Maven build | 7 días (artifact) |
| Coverage (JaCoCo) | Test stage | 30 días |
| OWASP Report | Dependency Check | 30 días |
| Release Notes | Semantic version job | Inclusión en release |

## 10. Metrics & Continuous Improvement
- Medir tiempo desde PR abierto a merge (Lead Time).
- Ratio de Rollbacks sobre releases totales (objetivo <5%).
- Vulnerabilidades críticas por release (objetivo 0).

## 11. Manual Trigger / Emergency Release
- Usar `workflow_dispatch` con `environment=prod` para releases urgentes.
- Documentar Issue con: causa, cambios mínimos, riesgo y plan de rollback.

## 12. Post-Release Review
- Revisar logs (5-15 min post deploy).
- Confirmar health endpoints `/actuator/health` en cada servicio.
- Registrar conclusiones en Issue etiquetado `post-release`.

## 13. Future Improvements
- Integrar `git-cliff` o `conventional-changelog` para notas enriquecidas.
- Añadir firma de tags (GPG) para mayor trazabilidad.
- Publicar dif HTML comparativa (GitHub Actions artifact).

---
Este documento asegura cumplimiento del criterio "Change Management y Release Notes".
