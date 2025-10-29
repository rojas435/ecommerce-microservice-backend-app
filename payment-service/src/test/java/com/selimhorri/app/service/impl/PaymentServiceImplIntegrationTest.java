package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.PaymentService;

/**
 * Integration Tests for PaymentServiceImpl
 * Tests service communication with Order microservice
 * Uses @MockBean to mock RestTemplate calls to external services
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PaymentService Integration Tests")
class PaymentServiceImplIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private RestTemplate restTemplate;

    private OrderDto testOrderDto;

    /**
     * Setup method executed before each test
     * Initializes test data and cleans database
     */
    @BeforeEach
    void setUp() {
        // Clean database
        paymentRepository.deleteAll();

        // Reset mocks
        reset(restTemplate);

        // Setup test OrderDto
        testOrderDto = new OrderDto();
        testOrderDto.setOrderId(1);
        testOrderDto.setOrderDate(LocalDateTime.now());
        testOrderDto.setOrderDesc("Test Order Description");
        testOrderDto.setOrderFee(50.0);
    }

    /**
     * Test findAll() method
     * Validates that the service calls Order service and enriches payments
     * 
     * Expected: 1 payment returned with enriched order data
     */
    @Test
    @DisplayName("findAll() should call Order service and return enriched payments")
    void testFindAll_CallsOrderService_ReturnsEnrichedPayments() {
        // Arrange: Save a payment to database
        Payment payment = new Payment();
        payment.setOrderId(1);
        payment.setIsPayed(true);
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        // Mock RestTemplate call to Order service
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"), 
                eq(OrderDto.class)))
            .thenReturn(testOrderDto);

        // Act: Call service method
        List<PaymentDto> payments = paymentService.findAll();

        // Assert: Verify results
        assertNotNull(payments, "Payments list should not be null");
        assertEquals(1, payments.size(), "Should return 1 payment");

        PaymentDto paymentDto = payments.get(0);
        assertTrue(paymentDto.getIsPayed(), "Payment should be marked as payed");
        assertEquals(PaymentStatus.COMPLETED, paymentDto.getPaymentStatus(), "Payment status should match");

        // Verify external service call was made
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"), 
                eq(OrderDto.class));
    }

    /**
     * Test findById() method
     * Validates that the service calls Order service for a single payment
     * 
     * Expected: Single payment returned with enriched order data
     */
    @Test
    @DisplayName("findById() should call Order service and return enriched payment")
    void testFindById_CallsOrderService_ReturnsEnrichedPayment() {
        // Arrange: Save a payment to database
        Payment payment = new Payment();
        payment.setOrderId(1);
        payment.setIsPayed(false);
        payment.setPaymentStatus(PaymentStatus.IN_PROGRESS);
        Payment savedPayment = paymentRepository.save(payment);

        // Mock RestTemplate call to Order service
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"), 
                eq(OrderDto.class)))
            .thenReturn(testOrderDto);

        // Act: Call service method
        PaymentDto result = paymentService.findById(savedPayment.getPaymentId());

        // Assert: Verify results
        assertNotNull(result, "Result should not be null");
        assertEquals(savedPayment.getPaymentId(), result.getPaymentId(), "Payment ID should match");
        assertFalse(result.getIsPayed(), "Payment should not be marked as payed");
        assertEquals(PaymentStatus.IN_PROGRESS, result.getPaymentStatus(), "Payment status should match");

        // Verify external service call was made
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"), 
                eq(OrderDto.class));
    }

    /**
     * Test save() method
     * Validates that save operation does NOT call external services (performance optimization)
     * 
     * Expected: Payment saved successfully without external HTTP calls
     */
    @Test
    @DisplayName("save() should create payment without calling external services")
    void testSave_CreatesPayment_WithoutExternalCalls() {
        // Arrange: Create new PaymentDto with OrderDto
        OrderDto newOrderDto = new OrderDto();
        newOrderDto.setOrderId(2);
        
        PaymentDto newPaymentDto = new PaymentDto();
        newPaymentDto.setOrderDto(newOrderDto);
        newPaymentDto.setIsPayed(true);
        newPaymentDto.setPaymentStatus(PaymentStatus.COMPLETED);

        // Act: Save payment
        PaymentDto result = paymentService.save(newPaymentDto);

        // Assert: Verify saved
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getPaymentId(), "Payment ID should be generated");
        assertTrue(result.getIsPayed(), "Payment should be marked as payed");
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus(), "Payment status should match");

        // Verify NO external service calls were made (performance optimization)
        verify(restTemplate, never()).getForObject(anyString(), any());

        // Verify database persistence
        assertTrue(paymentRepository.existsById(result.getPaymentId()), 
                "Payment should exist in database");
    }
}
