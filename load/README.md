# Pruebas de carga (k6) — Fase 2 del Plan de Capacidad

Scripts de carga para validar el SLO (p95 < 500 ms, error rate < 1%) antes de la campaña ×10.
Ver `Plan_Capacidad_Rendimiento_SafeIndustrial.md` §3.

## Instalar k6

```powershell
winget install k6 --source winget
# o: choco install k6
```

## Scripts

| Script | Qué prueba | SLO |
|--------|-----------|-----|
| `lt01-login-keycloak.js` | Login masivo (CPU de Keycloak, hashing PBKDF2) | p95 token < 1 s |
| `lt02-api-sync.js` | APIs síncronas vía gateway con JWT (el ×10) | p95 < 500 ms, error < 1% |

## Cómo ejecutar

```powershell
# LT-02: carga de APIs (ajusta las URLs y credenciales de prueba)
k6 run `
  -e GATEWAY_URL=https://tu-gateway.ngrok.app `
  -e KEYCLOAK_URL=https://tu-keycloak `
  -e CLIENT_ID=web `
  -e TEST_USER=demo `
  -e TEST_PASS=demo `
  load/lt02-api-sync.js
```

k6 termina con **exit code != 0** si no se cumplen los `thresholds` → sirve como *gate* en el pipeline.

## Reglas

- Ejecutar contra **staging**, nunca producción.
- Siempre atravesar el **api-gateway con token real** de Keycloak (si se omite la auth, los resultados no sirven).
- Sembrar datos sintéticos en la BD antes de LT-04 (consultas pesadas).
