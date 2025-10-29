#!/bin/bash

# Port-Forward Script para E2E Tests (Linux/Mac)
# Este script inicia port-forwards para todos los microservicios necesarios

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}🚀 Iniciando Port-Forwards para E2E Tests...${NC}"

# Verificar que kubectl esté disponible
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}❌ kubectl no encontrado. Por favor instálalo primero.${NC}"
    exit 1
fi

# Verificar que Minikube esté corriendo
if ! minikube status &> /dev/null; then
    echo -e "${RED}❌ Minikube no está corriendo. Ejecuta: minikube start${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Minikube está activo${NC}"

# Verificar namespace ecommerce
if ! kubectl get namespace ecommerce &> /dev/null; then
    echo -e "${RED}❌ Namespace 'ecommerce' no encontrado${NC}"
    echo -e "${YELLOW}Crea el namespace con: kubectl create namespace ecommerce${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Namespace 'ecommerce' encontrado${NC}"

# Array de PIDs de procesos port-forward
declare -a PF_PIDS=()

# Función de limpieza al salir
cleanup() {
    echo -e "\n${YELLOW}🛑 Deteniendo port-forwards...${NC}"
    for pid in "${PF_PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
        fi
    done
    echo -e "${GREEN}✅ Port-forwards detenidos${NC}"
    exit 0
}

# Registrar función de cleanup para Ctrl+C
trap cleanup SIGINT SIGTERM EXIT

# Función para iniciar port-forward
start_port_forward() {
    local service_name=$1
    local port=$2
    
    echo -e "${YELLOW}🔌 Port-forwarding: $service_name → localhost:$port${NC}"
    
    # Iniciar kubectl port-forward en background
    kubectl port-forward -n ecommerce service/$service_name $port:$port &> /dev/null &
    local pid=$!
    PF_PIDS+=($pid)
    
    # Esperar un momento para que se establezca la conexión
    sleep 2
    
    # Verificar que el proceso siga vivo
    if kill -0 "$pid" 2>/dev/null; then
        echo -e "${GREEN}✅ $service_name escuchando en puerto $port (PID: $pid)${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name falló al iniciar port-forward${NC}"
        return 1
    fi
}

# Verificar que los pods estén corriendo
echo -e "\n${CYAN}📦 Verificando pods en namespace ecommerce...${NC}"
running_pods=$(kubectl get pods -n ecommerce --no-headers 2>/dev/null | grep -c "Running" || echo "0")

echo -e "${GREEN}✅ $running_pods pods en estado Running${NC}"

if [ "$running_pods" -eq 0 ]; then
    echo -e "${RED}❌ No hay pods corriendo. Despliega los servicios primero.${NC}"
    exit 1
fi

# Iniciar port-forwards para cada servicio
echo -e "\n${CYAN}🔌 Iniciando port-forwards...${NC}"

declare -a services=(
    "product-service:8500"
    "user-service:8400"
    "order-service:8300"
    "payment-service:8600"
    "shipping-service:8700"
    "favourite-service:8800"
    "api-gateway:8080"
)

success_count=0
total_services=${#services[@]}

for service_port in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_port"
    if start_port_forward "$service" "$port"; then
        ((success_count++))
    fi
done

echo -e "\n${GREEN}✅ $success_count/$total_services port-forwards activos${NC}"

# Mostrar URLs disponibles
echo -e "\n${CYAN}🌐 URLs Disponibles:${NC}"
echo "  - API Gateway:       http://localhost:8080"
echo "  - Product Service:   http://localhost:8500/api/products"
echo "  - User Service:      http://localhost:8400/api/users"
echo "  - Order Service:     http://localhost:8300/api/orders"
echo "  - Payment Service:   http://localhost:8600/api/payments"
echo "  - Shipping Service:  http://localhost:8700/api/order-items"
echo "  - Favourite Service: http://localhost:8800/api/favourites"

# Instrucciones para ejecutar tests
echo -e "\n${CYAN}🧪 Para ejecutar los E2E tests:${NC}"
echo -e "${YELLOW}  cd e2e-tests${NC}"
echo -e "${YELLOW}  mvn clean test${NC}"

# Mantener el script corriendo
echo -e "\n${GREEN}⏸️  Port-forwards activos. Presiona Ctrl+C para detener...${NC}"
echo ""

# Monitorear procesos
while true; do
    sleep 5
    
    # Contar procesos vivos
    alive_count=0
    for pid in "${PF_PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            ((alive_count++))
        fi
    done
    
    if [ "$alive_count" -lt "$total_services" ]; then
        echo -e "${YELLOW}⚠️  Algunos port-forwards se detuvieron ($alive_count/$total_services activos)${NC}"
    fi
done
