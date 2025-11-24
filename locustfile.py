"""
Locust Performance Tests for E-commerce Microservices
======================================================

This file defines performance tests for the e-commerce backend application.
Locust will simulate concurrent users performing various operations.

Metrics measured:
- Response time (p50, p95, p99)
- Requests per second (RPS)
- Error rate
- Success rate

Usage:
    # Local execution (with port-forward to API Gateway)
    locust -f locustfile.py --host=http://localhost:8080

    # Headless mode (CI/CD)
    locust -f locustfile.py --host=http://api-gateway:8080 \
           --users 100 --spawn-rate 10 --run-time 3m \
           --headless --html performance-report.html

Environment toggle:
    LOCUST_ROUTING_MODE=service-prefix (default) uses /order-service/... routes
    LOCUST_ROUTING_MODE=api-prefix    targets /api/... routes used in docker
"""

from locust import HttpUser, task, between, SequentialTaskSet
import os
import random
import json
from datetime import datetime

# Allow switching between legacy service-prefixed routes (/order-service/...) and
# the api-only routes (/api/orders) used by the docker profile.
ROUTING_MODE = os.getenv("LOCUST_ROUTING_MODE", "service-prefix").strip().lower()


def build_path(service_segment: str, endpoint: str) -> str:
    """Return the correct gateway path based on the routing mode."""
    clean_endpoint = endpoint.lstrip('/')
    if ROUTING_MODE in {"api", "api-prefix", "api_only"}:
        return f"/{clean_endpoint}"
    clean_service = service_segment.strip('/\\')
    return f"/{clean_service}/{clean_endpoint}"


class EcommerceUserBehavior(SequentialTaskSet):
    """
    Sequential task set representing a typical user journey:
    1. Browse products
    2. View product details
    3. Add to favourites
    4. Create order
    5. View orders
    """
    
    # Shared data between tasks
    product_id = None
    order_id = None
    user_id = 1  # Using fixed user ID for simplicity in tests
    
    @task
    def browse_products(self):
        """
        GET /product-service/api/products
        Most common operation - 40% of traffic
        Expected: < 200ms response time
        """
        with self.client.get(
            build_path("product-service", "api/products"),
            catch_response=True,
            name="Browse Products"
        ) as response:
            if response.status_code == 200:
                try:
                    products = response.json()
                    if products and len(products) > 0:
                        # Store random product ID for next tasks
                        self.product_id = products[random.randint(0, min(len(products)-1, 10))].get('productId', 1)
                        response.success()
                    else:
                        response.failure("No products returned")
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Got status code {response.status_code}")
    
    @task
    def view_product_details(self):
        """
        GET /product-service/api/products/{id}
        View specific product - 25% of traffic
        Expected: < 200ms response time
        """
        if not self.product_id:
            self.product_id = random.randint(1, 10)
        
        with self.client.get(
            f"{build_path('product-service', 'api/products')}/{self.product_id}",
            catch_response=True,
            name="View Product Details"
        ) as response:
            if response.status_code == 200:
                try:
                    product = response.json()
                    if product.get('productId'):
                        response.success()
                    else:
                        response.failure("Invalid product data")
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            elif response.status_code == 404:
                # Product not found is acceptable, try another
                self.product_id = random.randint(1, 10)
                response.success()  # Don't count as failure
            else:
                response.failure(f"Got status code {response.status_code}")
    
    @task
    def add_to_favourites(self):
        """
        POST /favourite-service/api/favourites
        Add product to favourites - 10% of traffic
        Expected: < 300ms response time
        """
        if not self.product_id:
            self.product_id = random.randint(1, 10)
        
        payload = {
            "userId": self.user_id,
            "productId": self.product_id,
            "likeDate": datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
        }
        
        with self.client.post(
            build_path("favourite-service", "api/favourites"),
            json=payload,
            catch_response=True,
            name="Add to Favourites"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            elif response.status_code == 409:
                # Conflict (already exists) is acceptable
                response.success()
            else:
                response.failure(f"Got status code {response.status_code}")
    
    @task
    def create_order(self):
        """
        POST /order-service/api/orders
        Create new order - 15% of traffic
        Expected: < 500ms response time (writes are slower)
        """
        if not self.product_id:
            self.product_id = random.randint(1, 10)
        
        payload = {
            "orderDate": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "orderDesc": f"Performance test order {random.randint(1000, 9999)}",
            "orderFee": round(random.uniform(10.0, 500.0), 2),
            "userId": self.user_id
        }
        
        with self.client.post(
            build_path("order-service", "api/orders"),
            json=payload,
            catch_response=True,
            name="Create Order"
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    order = response.json()
                    self.order_id = order.get('orderId')
                    response.success()
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Got status code {response.status_code}")
    
    @task
    def view_orders(self):
        """
        GET /order-service/api/orders
        View user's orders - 10% of traffic
        Expected: < 250ms response time
        """
        with self.client.get(
            build_path("order-service", "api/orders"),
            catch_response=True,
            name="View Orders"
        ) as response:
            if response.status_code == 200:
                try:
                    orders = response.json()
                    response.success()
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Got status code {response.status_code}")


class ReadHeavyUser(HttpUser):
    """
    User that mostly reads (browses products, views details)
    Represents 70% of real users (shoppers browsing)
    Weight: 7
    """
    wait_time = between(1, 3)  # Wait 1-3 seconds between requests (human-like)
    weight = 7
    
    @task(10)  # 10x more likely than other tasks
    def browse_products(self):
        """Browse products list"""
        self.client.get(build_path("product-service", "api/products"), name="[Read] Browse Products")
    
    @task(5)
    def view_product(self):
        """View random product details"""
        product_id = random.randint(1, 20)
        self.client.get(f"{build_path('product-service', 'api/products')}/{product_id}", name="[Read] View Product")
    
    @task(2)
    def view_orders(self):
        """View user's orders"""
        self.client.get(build_path("order-service", "api/orders"), name="[Read] View Orders")


class WriteHeavyUser(HttpUser):
    """
    User that performs write operations (creates orders, adds favourites)
    Represents 30% of real users (active buyers)
    Weight: 3
    """
    wait_time = between(2, 5)  # Slower pace for write operations
    weight = 3
    
    @task(5)
    def create_order(self):
        """Create a new order"""
        payload = {
            "orderDate": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "orderDesc": f"Load test order {random.randint(1000, 9999)}",
            "orderFee": round(random.uniform(50.0, 500.0), 2),
            "userId": random.randint(1, 10)
        }
        self.client.post(build_path("order-service", "api/orders"), json=payload, name="[Write] Create Order")
    
    @task(3)
    def add_favourite(self):
        """Add product to favourites"""
        payload = {
            "userId": random.randint(1, 10),
            "productId": random.randint(1, 20),
            "likeDate": datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
        }
        self.client.post(build_path("favourite-service", "api/favourites"), json=payload, name="[Write] Add Favourite")
    
    @task(2)
    def create_payment(self):
        """Create a payment"""
        payload = {
            "isPayed": True,
            "paymentDate": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "orderId": random.randint(1, 100)
        }
        self.client.post(build_path("payment-service", "api/payments"), json=payload, name="[Write] Create Payment")


class RealisticUserJourney(HttpUser):
    """
    Realistic user following a complete shopping journey
    Uses SequentialTaskSet for ordered execution
    Weight: 2 (20% of users follow complete journey)
    """
    wait_time = between(1, 4)
    weight = 2
    tasks = [EcommerceUserBehavior]


# Performance thresholds for CI/CD validation
# These can be checked by parsing the HTML report
PERFORMANCE_THRESHOLDS = {
    "Browse Products": {
        "max_avg_response_time": 200,  # ms
        "max_p95_response_time": 500,
        "max_error_rate": 1.0  # percentage
    },
    "View Product Details": {
        "max_avg_response_time": 200,
        "max_p95_response_time": 500,
        "max_error_rate": 2.0
    },
    "Create Order": {
        "max_avg_response_time": 500,
        "max_p95_response_time": 1000,
        "max_error_rate": 1.0
    },
    "Add to Favourites": {
        "max_avg_response_time": 300,
        "max_p95_response_time": 800,
        "max_error_rate": 1.0
    }
}
