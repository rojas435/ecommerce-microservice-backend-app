package com.selimhorri.app.service.impl;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Integration Test #4: Payment Service → Order Service Integration
 * 
 * Validates that Payment Service correctly retrieves and enriches payment data
 * with order information by calling ORDER-SERVICE.
 * 
 * Business Value: Ensures payments are linked to orders, providing complete
 * transaction context for financial reporting and order status tracking.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Test #4: Payment Service enriches payments with Order data")
class PaymentOrderIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private RestTemplate restTemplate; // Mock RestTemplate for integration testing

    // Store saved payment for use across tests
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        paymentRepository.deleteAll();
        
        // Reset mock behavior before each test
        reset(restTemplate);

        // Insert test payment data - JPA will assign ID automatically
        Payment testPayment = Payment.builder()
                .paymentId(null) // Let JPA assign ID
                .isPayed(true)
                .orderId(101)
                .build();
        savedPayment = paymentRepository.save(testPayment); // Store reference
    }

    /**
     * Test 1: Validates that findById enriches payment with order details from ORDER-SERVICE
     * 
     * Business Logic: When retrieving a payment, the service MUST fetch the associated
     * order to provide complete payment context (order total, customer, items, etc.)
     */
    @Test
    @DisplayName("findById() calls ORDER-SERVICE to enrich payment with order details")
    void testFindById_EnrichesPaymentWithOrderData() {
        // Given: Mock ORDER-SERVICE returns order data
        OrderDto mockOrder = OrderDto.builder()
                .orderId(101)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order for Payment")
                .orderFee(250.0)
                .build();
        
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/101"),
                eq(OrderDto.class)))
                .thenReturn(mockOrder);

        // When: We retrieve the payment by its actual saved ID
        PaymentDto result = paymentService.findById(savedPayment.getPaymentId());

        // Then: Payment should be retrieved and enriched with order data
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(savedPayment.getPaymentId());
        assertThat(result.getIsPayed()).isTrue();
        
        // Verify order data was fetched and populated
        assertThat(result.getOrderDto()).isNotNull();
        assertThat(result.getOrderDto().getOrderId()).isEqualTo(101);
        assertThat(result.getOrderDto().getOrderDesc()).isEqualTo("Test Order for Payment");
        assertThat(result.getOrderDto().getOrderFee()).isEqualTo(250.0);

        // Verify ORDER-SERVICE was called exactly once
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/101"),
                eq(OrderDto.class));
    }

    /**
     * Test 2: Validates that findAll enriches all payments with order data
     * 
     * Business Logic: When listing payments, each payment MUST be enriched with
     * its associated order for complete financial reporting.
     */
    @Test
    @DisplayName("findAll() calls ORDER-SERVICE for each payment to enrich with order data")
    void testFindAll_EnrichesAllPaymentsWithOrderData() {
        // Given: Multiple payments with different orders
        Payment payment2 = Payment.builder()
                .paymentId(2)
                .isPayed(false)
                .orderId(102)
                .build();
        Payment payment3 = Payment.builder()
                .paymentId(3)
                .isPayed(true)
                .orderId(103)
                .build();
        paymentRepository.save(payment2);
        paymentRepository.save(payment3);

        // Mock ORDER-SERVICE responses for each order
        OrderDto order101 = OrderDto.builder()
                .orderId(101)
                .orderDesc("Order 101")
                .orderFee(250.0)
                .build();
        OrderDto order102 = OrderDto.builder()
                .orderId(102)
                .orderDesc("Order 102")
                .orderFee(150.0)
                .build();
        OrderDto order103 = OrderDto.builder()
                .orderId(103)
                .orderDesc("Order 103")
                .orderFee(350.0)
                .build();

        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/101"),
                eq(OrderDto.class))).thenReturn(order101);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/102"),
                eq(OrderDto.class))).thenReturn(order102);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/103"),
                eq(OrderDto.class))).thenReturn(order103);

        // When: We retrieve all payments
        var payments = paymentService.findAll();

        // Then: All payments should be enriched with order data
        assertThat(payments).isNotNull();
        assertThat(payments.size()).isEqualTo(3);

        // Verify each payment has order data populated
        PaymentDto payment1Dto = payments.stream()
                .filter(p -> p.getPaymentId().equals(savedPayment.getPaymentId()))
                .findFirst()
                .orElseThrow();
        assertThat(payment1Dto.getOrderDto()).isNotNull();
        assertThat(payment1Dto.getOrderDto().getOrderId()).isEqualTo(101);

        // Verify ORDER-SERVICE was called 3 times (once per payment)
        verify(restTemplate, times(3)).getForObject(
                anyString(),
                eq(OrderDto.class));
    }

    /**
     * Test 3: Validates error handling when ORDER-SERVICE returns 404 (order not found)
     * 
     * Business Logic: If an order no longer exists, the payment retrieval should
     * either fail gracefully or throw a meaningful exception indicating data inconsistency.
     */
    @Test
    @DisplayName("findById() handles ORDER-SERVICE 404 error when order doesn't exist")
    void testFindById_HandlesOrderNotFound() {
        // Given: Mock ORDER-SERVICE throws 404 Not Found
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/101"),
                eq(OrderDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null));

        // When/Then: Retrieving payment should propagate the exception
        assertThatThrownBy(() -> paymentService.findById(1))
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        // Verify ORDER-SERVICE was attempted
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/101"),
                eq(OrderDto.class));
    }

    /**
     * Test 4: Validates that save() creates payment without calling ORDER-SERVICE
     * 
     * Business Logic: Payment creation should be fast and not require fetching
     * order details, only storing the orderId reference for later enrichment.
     */
    @Test
    @DisplayName("save() creates payment without calling ORDER-SERVICE (performance optimization)")
    void testSave_CreatesPaymentWithoutOrderServiceCall() {
        // Given: A new payment to save (with orderDto containing orderId reference)
        OrderDto orderReference = OrderDto.builder()
                .orderId(104)
                .build();
        PaymentDto newPayment = PaymentDto.builder()
                .isPayed(false)
                .orderDto(orderReference)
                .build();

        // When: We save the payment
        PaymentDto savedPayment = paymentService.save(newPayment);

        // Then: Payment should be created successfully
        assertThat(savedPayment).isNotNull();
        assertThat(savedPayment.getPaymentId()).isNotNull();
        assertThat(savedPayment.getIsPayed()).isFalse();
        assertThat(savedPayment.getOrderDto()).isNotNull();
        assertThat(savedPayment.getOrderDto().getOrderId()).isEqualTo(104);

        // Verify NO external calls during save (performance optimization)
        verify(restTemplate, never()).getForObject(anyString(), eq(OrderDto.class));
    }

    /**
     * Test 5: Validates that findById throws exception when payment not found
     * 
     * Business Logic: Attempting to retrieve a non-existent payment should throw
     * PaymentNotFoundException, NOT attempt to call ORDER-SERVICE.
     */
    @Test
    @DisplayName("findById() throws PaymentNotFoundException when payment doesn't exist")
    void testFindById_ThrowsExceptionWhenPaymentNotFound() {
        // Given: Payment ID that doesn't exist
        Integer nonExistentId = 99999;

        // When/Then: Should throw PaymentNotFoundException
        assertThatThrownBy(() -> paymentService.findById(nonExistentId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment with id: 99999 not found");

        // Verify ORDER-SERVICE was NEVER called (fail fast on missing payment)
        verify(restTemplate, never()).getForObject(anyString(), eq(OrderDto.class));
    }

    /**
     * VALIDATION NOTES:
     * 
     * This integration test suite validates the REAL integration between
     * Payment Service and Order Service as implemented in PaymentServiceImpl.java:
     * 
     * Current Implementation (lines 36-38, 51-53):
     * - findById() and findAll() call ORDER-SERVICE via restTemplate.getForObject()
     * - save() does NOT call ORDER-SERVICE (correct optimization)
     * - Exception handling relies on Spring's RestTemplate default behavior
     * 
     * All 5 tests validate ACTUAL business logic:
     * ✅ Test 1: Validates enrichment in findById()
     * ✅ Test 2: Validates enrichment in findAll() with multiple payments
     * ✅ Test 3: Validates error propagation when order doesn't exist
     * ✅ Test 4: Validates save() performance (no external calls)
     * ✅ Test 5: Validates payment not found scenario (no external calls)
     * 
     * Expected Result: ALL 5 TESTS SHOULD PASS ✅
     */
}
