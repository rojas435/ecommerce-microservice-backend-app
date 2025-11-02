#!/bin/bash
#
# Performance Test Runner for E-commerce Application
# ===================================================
#
# This script runs Locust performance tests against the API Gateway.
# Can be executed locally (with port-forward) or in Kubernetes (via Job).
#
# Usage:
#   ./run-performance-tests.sh [local|k8s] [users] [duration]
#
# Examples:
#   ./run-performance-tests.sh local 50 2m     # Local test: 50 users for 2 minutes
#   ./run-performance-tests.sh k8s 100 5m      # K8s test: 100 users for 5 minutes
#

set -e

# Default values
MODE=${1:-local}
USERS=${2:-100}
DURATION=${3:-3m}
SPAWN_RATE=$((USERS / 10))  # Spawn 10% of users per second

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  Performance Test Runner${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""
echo -e "Mode:       ${GREEN}${MODE}${NC}"
echo -e "Users:      ${GREEN}${USERS}${NC}"
echo -e "Duration:   ${GREEN}${DURATION}${NC}"
echo -e "Spawn Rate: ${GREEN}${SPAWN_RATE}/s${NC}"
echo ""

# Check if locust is installed
if ! command -v locust &> /dev/null; then
    echo -e "${RED}ERROR: Locust is not installed!${NC}"
    echo ""
    echo "Install it with:"
    echo "  pip install locust"
    exit 1
fi

# Check if locustfile.py exists
if [ ! -f "locustfile.py" ]; then
    echo -e "${RED}ERROR: locustfile.py not found!${NC}"
    echo "Make sure you're in the project root directory."
    exit 1
fi

# Function to run tests locally
run_local_test() {
    echo -e "${YELLOW}Starting LOCAL performance test...${NC}"
    echo ""
    echo -e "${YELLOW}IMPORTANT:${NC} Make sure you have port-forwarded the API Gateway:"
    echo "  kubectl port-forward -n ecommerce svc/api-gateway 8080:8080"
    echo ""
    
    # Wait for user confirmation
    read -p "Press Enter to continue once port-forward is ready..."
    
    # Check if API Gateway is accessible
    echo -e "${BLUE}Checking API Gateway accessibility...${NC}"
    if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo -e "${RED}ERROR: Cannot reach API Gateway at http://localhost:8080${NC}"
        echo "Make sure the port-forward is running."
        exit 1
    fi
    echo -e "${GREEN}✓ API Gateway is accessible${NC}"
    echo ""
    
    # Run locust
    echo -e "${BLUE}Running Locust test...${NC}"
    locust \
        --locustfile=locustfile.py \
        --host=http://localhost:8080 \
        --users=${USERS} \
        --spawn-rate=${SPAWN_RATE} \
        --run-time=${DURATION} \
        --headless \
        --html=reports/performance-report-local.html \
        --csv=reports/performance-local \
        --logfile=reports/locust.log
    
    echo ""
    echo -e "${GREEN}✓ Test completed!${NC}"
    echo ""
    echo "Reports generated:"
    echo "  - HTML: reports/performance-report-local.html"
    echo "  - CSV:  reports/performance-local_stats.csv"
    echo "  - Log:  reports/locust.log"
}

# Function to run tests in Kubernetes
run_k8s_test() {
    echo -e "${YELLOW}Starting KUBERNETES performance test...${NC}"
    echo ""
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}ERROR: kubectl is not installed!${NC}"
        exit 1
    fi
    
    # Check if namespace exists
    if ! kubectl get namespace ecommerce &> /dev/null; then
        echo -e "${RED}ERROR: Namespace 'ecommerce' does not exist!${NC}"
        exit 1
    fi
    
    # Check if API Gateway is running
    echo -e "${BLUE}Checking if API Gateway is running...${NC}"
    if ! kubectl get deployment api-gateway -n ecommerce &> /dev/null; then
        echo -e "${RED}ERROR: API Gateway deployment not found in namespace 'ecommerce'${NC}"
        exit 1
    fi
    
    READY=$(kubectl get deployment api-gateway -n ecommerce -o jsonpath='{.status.readyReplicas}')
    if [ "$READY" -eq "0" ] || [ -z "$READY" ]; then
        echo -e "${RED}ERROR: API Gateway is not ready!${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ API Gateway is running (${READY} replicas)${NC}"
    echo ""
    
    # Apply ConfigMap with locustfile
    echo -e "${BLUE}Creating locustfile ConfigMap...${NC}"
    kubectl create configmap locustfile \
        --from-file=locustfile.py \
        --namespace=ecommerce \
        --dry-run=client -o yaml | kubectl apply -f -
    echo -e "${GREEN}✓ ConfigMap created${NC}"
    echo ""
    
    # Delete previous job if exists
    if kubectl get job locust-performance-test -n ecommerce &> /dev/null; then
        echo -e "${YELLOW}Deleting previous test job...${NC}"
        kubectl delete job locust-performance-test -n ecommerce --wait=false
        sleep 5
    fi
    
    # Create and run the job
    echo -e "${BLUE}Creating performance test Job...${NC}"
    kubectl apply -f k8s/performance-test-job.yaml
    echo -e "${GREEN}✓ Job created${NC}"
    echo ""
    
    # Wait for job to complete
    echo -e "${BLUE}Waiting for test to complete...${NC}"
    kubectl wait --for=condition=complete --timeout=600s job/locust-performance-test -n ecommerce || true
    
    # Get job status
    JOB_STATUS=$(kubectl get job locust-performance-test -n ecommerce -o jsonpath='{.status.conditions[0].type}')
    
    if [ "$JOB_STATUS" == "Complete" ]; then
        echo -e "${GREEN}✓ Test completed successfully!${NC}"
    else
        echo -e "${RED}✗ Test failed or timed out!${NC}"
        echo ""
        echo "Check logs with:"
        echo "  kubectl logs -n ecommerce job/locust-performance-test"
        exit 1
    fi
    
    # Get pod name
    POD_NAME=$(kubectl get pods -n ecommerce -l job-name=locust-performance-test -o jsonpath='{.items[0].metadata.name}')
    
    echo ""
    echo -e "${BLUE}Test logs:${NC}"
    kubectl logs -n ecommerce $POD_NAME
    
    echo ""
    echo -e "${YELLOW}To extract reports, run:${NC}"
    echo "  kubectl cp ecommerce/${POD_NAME}:/reports/performance-report.html ./reports/performance-report-k8s.html"
    echo "  kubectl cp ecommerce/${POD_NAME}:/reports/performance_stats.csv ./reports/performance-k8s_stats.csv"
}

# Create reports directory
mkdir -p reports

# Run test based on mode
case $MODE in
    local)
        run_local_test
        ;;
    k8s|kubernetes)
        run_k8s_test
        ;;
    *)
        echo -e "${RED}ERROR: Invalid mode '${MODE}'${NC}"
        echo ""
        echo "Usage: $0 [local|k8s] [users] [duration]"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Performance Test Complete!${NC}"
echo -e "${GREEN}======================================${NC}"
