# Seguridad

Este folder centraliza los manifiestos que habilitan RBAC y TLS para los recursos expuestos públicamente.

## RBAC para API Gateway

1. Crear los objetos necesarios:
   ```bash
   kubectl apply -f k8s/security/api-gateway-rbac.yaml
   ```
2. El Deployment (`k8s/api-gateway.yaml`) ya referencia el ServiceAccount `gateway-sa`, por lo que el pod solo puede leer los ConfigMaps y Services listados en el `Role`.

## TLS para servicios públicos

1. Genera o importa un certificado (puede ser auto-firmado para pruebas):
   ```bash
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout api-gateway.key -out api-gateway.crt \
     -subj "/CN=api.ecommerce.local/O=Ecommerce"
   kubectl -n ecommerce create secret tls api-gateway-tls \
     --cert=api-gateway.crt --key=api-gateway.key
   ```
2. Aplica el Ingress TLS:
   ```bash
   kubectl apply -f k8s/security/api-gateway-ingress.yaml
   ```
3. En tu máquina local agrega `api.ecommerce.local` a `/etc/hosts` (o equivalente) apuntando al IP del Ingress Controller.

> Para producción reemplaza el host por tu dominio real y usa el certificado emitido por tu CA/Let’s Encrypt.
