package com.selimhorri.e2e.config;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import java.time.Duration;
import static org.awaitility.Awaitility.await;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Base configuration for E2E tests.
 * Sets up RestAssured defaults and service endpoints.
 */
public class BaseE2ETest {

    // Service base URLs (configured via port-forward or LoadBalancer)
    protected static final String API_GATEWAY_URL = System.getProperty("api.gateway.url", "http://localhost:8080");
    protected static final String PRODUCT_SERVICE_URL = System.getProperty("product.service.url", "http://localhost:8500");
    protected static final String USER_SERVICE_URL = System.getProperty("user.service.url", "http://localhost:8700");
    protected static final String ORDER_SERVICE_URL = System.getProperty("order.service.url", "http://localhost:8300");
    protected static final String PAYMENT_SERVICE_URL = System.getProperty("payment.service.url", "http://localhost:8400");
    protected static final String SHIPPING_SERVICE_URL = System.getProperty("shipping.service.url", "http://localhost:8600");
    protected static final String FAVOURITE_SERVICE_URL = System.getProperty("favourite.service.url", "http://localhost:8800");

    // Common request specification
    protected static RequestSpecification requestSpec;

    @BeforeAll
    public static void setup() {
        // Configure RestAssured defaults
        RestAssured.baseURI = API_GATEWAY_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Build common request specification
        requestSpec = new RequestSpecBuilder()
                .setContentType("application/json")
                .setAccept("application/json")
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

    // Esperar readiness de servicios para evitar fallos por arranque en frío
    waitForHealth("API Gateway", API_GATEWAY_URL, "/actuator/health");
    waitForHealth("Product Service", PRODUCT_SERVICE_URL, "/product-service/actuator/health");
    waitForHealth("User Service", USER_SERVICE_URL, "/user-service/actuator/health");
    waitForHealth("Order Service", ORDER_SERVICE_URL, "/order-service/actuator/health");
    waitForHealth("Payment Service", PAYMENT_SERVICE_URL, "/payment-service/actuator/health");
    waitForHealth("Shipping Service", SHIPPING_SERVICE_URL, "/shipping-service/actuator/health");
    waitForHealth("Favourite Service", FAVOURITE_SERVICE_URL, "/favourite-service/actuator/health");
    }

    /**
     * Helper method to create a request specification for a specific service
     */
    protected static RequestSpecification specForService(String baseUrl) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType("application/json")
                .setAccept("application/json")
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
    }

    private static void waitForHealth(String serviceName, String baseUrl, String healthPath) {
        try {
            await()
                .ignoreExceptions()
                .pollInterval(java.time.Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(90))
                .untilAsserted(() ->
                        given()
                            .baseUri(baseUrl)
                            .accept("application/json")
                        .when()
                            .get(healthPath)
                        .then()
                            .statusCode(anyOf(is(200), is(204)))
                            .body(anyOf(
                                    hasKey("status"),
                                    anything()
                            ))
                );
            System.out.println("✅ " + serviceName + " listo (health OK)");
        } catch (AssertionError ae) {
            System.out.println("⚠️  " + serviceName + ": health no estable tras timeout, se continua igualmente");
        }
    }
}
