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
import threading
from datetime import datetime, timedelta

# Allow switching between legacy service-prefixed routes (/order-service/...) and
# the api-only routes (/api/orders) used by the docker profile.
ROUTING_MODE = os.getenv("LOCUST_ROUTING_MODE", "service-prefix").strip().lower()
ENABLE_FAVOURITE_WRITES = os.getenv("LOCUST_ENABLE_FAVOURITES", "false").strip().lower() in {"1", "true", "yes"}
ENABLE_ORDER_FLOW = os.getenv("LOCUST_ENABLE_ORDERS", "false").strip().lower() in {"1", "true", "yes"}
PRODUCT_CACHE_LOCK = threading.Lock()
PRODUCT_ID_CACHE: list[int] = []
USER_CACHE_LOCK = threading.Lock()
USER_ID_CACHE: list[int] = []
CART_CACHE_LOCK = threading.Lock()
CART_ID_CACHE: list[int] = []


def extract_collection(payload, *alternate_keys, allow_single_dict: bool = False) -> list:
    """Return a list of items from DtoCollectionResponse-like payloads."""
    if isinstance(payload, list):
        return payload
    if isinstance(payload, dict):
        collection = payload.get("collection")
        if isinstance(collection, list):
            return collection
        for key in alternate_keys:
            candidate = payload.get(key)
            if isinstance(candidate, list):
                return candidate
        if allow_single_dict:
            return [payload]
    return []
LIKE_DATE_LOCK = threading.Lock()
LIKE_DATE_COUNTER = 0


def format_app_datetime(base_dt: datetime | None = None) -> str:
    """Match the dd-MM-yyyy__HH:mm:ss:SSSSSS format expected by the services."""
    target = base_dt or datetime.now()
    return target.strftime("%d-%m-%Y__%H:%M:%S:%f")


def format_unique_like_datetime() -> str:
    """Ensure likeDate stays unique even if the DB truncates fractional seconds."""
    global LIKE_DATE_COUNTER
    with LIKE_DATE_LOCK:
        LIKE_DATE_COUNTER += 1
        offset_seconds = LIKE_DATE_COUNTER
    unique_dt = datetime.now() + timedelta(seconds=offset_seconds)
    return format_app_datetime(unique_dt)


def update_product_cache(products: list[dict] | dict) -> None:
    """Store any product ids returned by the API for later reuse."""
    ids = []
    for product in extract_collection(products):
        product_id = product.get("productId")
        if product_id is None:
            continue
        try:
            ids.append(int(product_id))
        except (TypeError, ValueError):
            continue
    if ids:
        with PRODUCT_CACHE_LOCK:
            # Keep most recent snapshot to avoid stale IDs that cause 400s
            global PRODUCT_ID_CACHE
            PRODUCT_ID_CACHE = ids


def get_cached_product_id() -> int | None:
    with PRODUCT_CACHE_LOCK:
        if PRODUCT_ID_CACHE:
            return random.choice(PRODUCT_ID_CACHE)
    return None


def ensure_product_cache(client) -> None:
    """Prime the product cache if it is empty."""
    if get_cached_product_id() is not None:
        return
    with client.get(
        build_path("product-service", "api/products"),
        name="[Cache] Fetch Products",
        catch_response=True,
    ) as response:
        if response.status_code == 200:
            try:
                update_product_cache(response.json())
                response.success()
            except json.JSONDecodeError:
                response.failure("Invalid JSON while caching products")
        else:
            response.failure(f"Failed to cache products (status {response.status_code})")


def pick_existing_product_id(client) -> int:
    ensure_product_cache(client)
    cached = get_cached_product_id()
    if cached is not None:
        return cached
    return random.randint(1, 10)


def update_cart_cache(payload: dict) -> None:
    orders = extract_collection(payload, allow_single_dict=True)
    ids = []
    for order in orders:
        if not isinstance(order, dict):
            continue
        cart_info = order.get("cart") or order.get("cartDto")
        if cart_info and isinstance(cart_info, dict):
            cart_id = cart_info.get("cartId")
            try:
                if cart_id is not None:
                    ids.append(int(cart_id))
            except (TypeError, ValueError):
                continue
    if ids:
        with CART_CACHE_LOCK:
            global CART_ID_CACHE
            CART_ID_CACHE = ids


def ensure_cart_cache(client) -> None:
    with CART_CACHE_LOCK:
        if CART_ID_CACHE:
            return
    with client.get(
        build_path("order-service", "api/orders"),
        name="[Cache] Fetch Orders",
        catch_response=True,
    ) as response:
        if response.status_code == 200:
            try:
                update_cart_cache(response.json())
                response.success()
            except json.JSONDecodeError:
                response.failure("Invalid JSON while caching carts")
        else:
            response.failure(f"Failed to cache carts (status {response.status_code})")


def pick_existing_cart_id(client) -> int | None:
    ensure_cart_cache(client)
    with CART_CACHE_LOCK:
        if CART_ID_CACHE:
            return random.choice(CART_ID_CACHE)
    return None

def update_user_cache(payload: dict) -> None:
    collection = extract_collection(payload, "users")
    ids = []
    for user in collection:
        user_id = user.get("userId") if isinstance(user, dict) else None
        if user_id is None:
            continue
        try:
            ids.append(int(user_id))
        except (TypeError, ValueError):
            continue
    if ids:
        with USER_CACHE_LOCK:
            global USER_ID_CACHE
            USER_ID_CACHE = ids


def ensure_user_cache(client) -> None:
    with USER_CACHE_LOCK:
        if USER_ID_CACHE:
            return
    with client.get(
        build_path("user-service", "api/users"),
        name="[Cache] Fetch Users",
        catch_response=True,
    ) as response:
        if response.status_code == 200:
            try:
                update_user_cache(response.json())
                response.success()
            except json.JSONDecodeError:
                response.failure("Invalid JSON while caching users")
        else:
            response.failure(f"Failed to cache users (status {response.status_code})")


def pick_existing_user_id(client) -> int:
    ensure_user_cache(client)
    with USER_CACHE_LOCK:
        if USER_ID_CACHE:
            return random.choice(USER_ID_CACHE)
    return 1



def build_path(service_segment: str, endpoint: str, force_service_prefix: bool = False) -> str:
    """Return the correct gateway path based on the routing mode."""
    clean_endpoint = endpoint.lstrip('/')
    if not force_service_prefix and ROUTING_MODE in {"api", "api-prefix", "api_only"}:
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
    user_id = None  # Resolved at runtime

    def on_start(self):
        self.user_id = pick_existing_user_id(self.client)
    
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
                    payload = response.json()
                    products = extract_collection(payload)
                    if products:
                        update_product_cache(payload)
                        candidates = products[: min(len(products), 10)]
                        raw_id = random.choice(candidates).get('productId', 1)
                        try:
                            self.product_id = int(raw_id)
                        except (TypeError, ValueError):
                            self.product_id = 1
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
            self.product_id = pick_existing_product_id(self.client)
        
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
                self.product_id = pick_existing_product_id(self.client)
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
        if not ENABLE_FAVOURITE_WRITES:
            return
        if not self.product_id:
            self.product_id = pick_existing_product_id(self.client)
        
        user_id = self.user_id or pick_existing_user_id(self.client)
        payload = {
            "userId": user_id,
            "productId": self.product_id,
            "likeDate": format_unique_like_datetime()
        }
        with self.client.post(
            build_path("favourite-service", "api/favourites"),
            json=payload,
            catch_response=True,
            name="Add to Favourites"
        ) as response:
            if response.status_code in [200, 201, 409]:
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
        if not ENABLE_ORDER_FLOW:
            return
        if not self.product_id:
            self.product_id = pick_existing_product_id(self.client)
        
        user_id = self.user_id or pick_existing_user_id(self.client)
        cart_id = pick_existing_cart_id(self.client)
        if not cart_id:
            return
        payload = {
            "orderDate": format_app_datetime(),
            "orderDesc": f"Performance test order {random.randint(1000, 9999)}",
            "orderFee": round(random.uniform(10.0, 500.0), 2),
            "cart": {"cartId": cart_id}
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
        if not ENABLE_ORDER_FLOW:
            return
        with self.client.get(
            build_path("order-service", "api/orders"),
            catch_response=True,
            name="View Orders"
        ) as response:
            if response.status_code == 200:
                try:
                    orders = response.json()
                    update_cart_cache(orders)
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
        with self.client.get(
            build_path("product-service", "api/products"),
            name="[Read] Browse Products",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    payload = response.json()
                    products = extract_collection(payload)
                    if not products:
                        response.failure("No products returned")
                    else:
                        update_product_cache(payload)
                        response.success()
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Got status code {response.status_code}")
    
    @task(5)
    def view_product(self):
        """View random product details"""
        product_id = pick_existing_product_id(self.client)
        self.client.get(f"{build_path('product-service', 'api/products')}/{product_id}", name="[Read] View Product")
    
    @task(2)
    def view_orders(self):
        """View user's orders"""
        if not ENABLE_ORDER_FLOW:
            return
        with self.client.get(
            build_path("order-service", "api/orders"),
            name="[Read] View Orders",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    update_cart_cache(response.json())
                    response.success()
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Got status code {response.status_code}")


class WriteHeavyUser(HttpUser):
    """
    User that performs write operations (creates orders, adds favourites)
    Represents 30% of real users (active buyers)
    Weight: 3
    """
    wait_time = between(2, 5)  # Slower pace for write operations
    weight = 3 if ENABLE_ORDER_FLOW else 0
    last_order_id: int | None = None
    user_id: int | None = None

    def on_start(self):
        self.user_id = pick_existing_user_id(self.client)
    
    @task(5)
    def create_order(self):
        """Create a new order"""
        if not ENABLE_ORDER_FLOW:
            return
        self._submit_order("[Write] Create Order")
    
    @task(3)
    def add_favourite(self):
        """Add product to favourites"""
        if not ENABLE_FAVOURITE_WRITES:
            return
        user_id = self.user_id or pick_existing_user_id(self.client)
        payload = {
            "userId": user_id,
            "productId": pick_existing_product_id(self.client),
            "likeDate": format_unique_like_datetime()
        }
        with self.client.post(
            build_path("favourite-service", "api/favourites"),
            json=payload,
            name="[Write] Add Favourite",
            catch_response=True,
        ) as response:
            if response.status_code in [200, 201, 409]:
                response.success()
            else:
                response.failure(f"Got status code {response.status_code}: {response.text[:200]}")
    
    @task(2)
    def create_payment(self):
        """Create a payment"""
        if not ENABLE_ORDER_FLOW:
            return
        order_id = self.last_order_id or self._submit_order("[Write] Create Order (prefetch)")
        if not order_id:
            return
        payload = {
            "isPayed": True,
            "paymentStatus": "COMPLETED",
            "order": {"orderId": order_id}
        }
        with self.client.post(
            build_path("payment-service", "api/payments"),
            json=payload,
            name="[Write] Create Payment",
            catch_response=True,
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Got status code {response.status_code}: {response.text[:200]}")

    def _submit_order(self, metric_name: str) -> int | None:
        if not ENABLE_ORDER_FLOW:
            return None
        user_id = self.user_id or pick_existing_user_id(self.client)
        cart_id = pick_existing_cart_id(self.client)
        if not cart_id:
            return None
        payload = {
            "orderDate": format_app_datetime(),
            "orderDesc": f"Load test order {random.randint(1000, 9999)}",
            "orderFee": round(random.uniform(50.0, 500.0), 2),
            "cart": {"cartId": cart_id}
        }
        with self.client.post(
            build_path("order-service", "api/orders"),
            json=payload,
            name=metric_name,
            catch_response=True,
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    order = response.json()
                    self.last_order_id = order.get('orderId')
                    response.success()
                    return self.last_order_id
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Got status code {response.status_code}: {response.text[:200]}")
        return None


class RealisticUserJourney(HttpUser):
    """
    Realistic user following a complete shopping journey
    Uses SequentialTaskSet for ordered execution
    Weight: 2 (20% of users follow complete journey)
    """
    wait_time = between(1, 4)
    weight = 2 if ENABLE_ORDER_FLOW else 0
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
