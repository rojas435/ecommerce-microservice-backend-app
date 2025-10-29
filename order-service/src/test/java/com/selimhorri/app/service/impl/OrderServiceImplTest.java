package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.OrderRepository;

/**
 * Unit Tests for OrderServiceImpl
 * Tests order management operations in isolation
 * Following DevOps best practices: Fast, Focused, Independent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;
    private Cart testCart;
    private CartDto testCartDto;

    /**
     * Setup method executed before each test
     * Initializes test data for order operations
     */
    @BeforeEach
    void setUp() {
        testCart = Cart.builder()
                .cartId(1)
                .build();
        
        testCartDto = CartDto.builder()
                .cartId(1)
                .build();
        
        testOrder = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .orderDesc("Test Order Description")
                .orderFee(50.0)
                .cart(testCart)
                .build();

        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .orderDesc("Test Order Description")
                .orderFee(50.0)
                .cartDto(testCartDto)
                .build();
    }

    /**
     * Business Value: Ensures orders can be retrieved from repository
     * Critical for: Order management dashboard, reporting
     */
    @Test
    @DisplayName("findAll() should return list of all orders")
    void testFindAll_ReturnsAllOrders() {
        // Arrange
        Cart cart2 = Cart.builder().cartId(2).build();
        
        Order order2 = Order.builder()
                .orderId(2)
                .orderDate(LocalDateTime.of(2024, 1, 16, 14, 0))
                .orderDesc("Second Order")
                .orderFee(75.0)
                .cart(cart2)
                .build();

        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder, order2));

        // Act
        List<OrderDto> result = orderService.findAll();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 orders");
        verify(orderRepository, times(1)).findAll();
    }

    /**
     * Business Value: Validates single order retrieval by ID
     * Critical for: Order details page, order tracking
     */
    @Test
    @DisplayName("findById() with valid ID should return order")
    void testFindById_ValidId_ReturnsOrder() {
        // Arrange
        Integer orderId = 1;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act
        OrderDto result = orderService.findById(orderId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(orderId, result.getOrderId(), "Order ID should match");
        assertEquals("Test Order Description", result.getOrderDesc(), "Order description should match");
        assertEquals(50.0, result.getOrderFee(), "Order fee should match");
        verify(orderRepository, times(1)).findById(orderId);
    }

    /**
     * Business Value: Ensures proper error handling for invalid order ID
     * Critical for: User feedback, error logging
     */
    @Test
    @DisplayName("findById() with invalid ID should throw OrderNotFoundException")
    void testFindById_InvalidId_ThrowsException() {
        // Arrange
        Integer invalidOrderId = 999;
        when(orderRepository.findById(invalidOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.findById(invalidOrderId);
        }, "Should throw OrderNotFoundException for non-existent order");

        verify(orderRepository, times(1)).findById(invalidOrderId);
    }

    /**
     * Business Value: Validates order creation in the system
     * Critical for: Checkout process, order placement
     */
    @Test
    @DisplayName("save() should create new order")
    void testSave_ValidOrder_ReturnsCreatedOrder() {
        // Arrange
        CartDto newCartDto = CartDto.builder().cartId(3).build();
        
        OrderDto newOrderDto = OrderDto.builder()
                .orderDate(LocalDateTime.of(2024, 1, 17, 9, 0))
                .orderDesc("New Order")
                .orderFee(100.0)
                .cartDto(newCartDto)
                .build();

        Cart newCart = Cart.builder().cartId(3).build();
        
        Order savedOrder = Order.builder()
                .orderId(3)
                .orderDate(LocalDateTime.of(2024, 1, 17, 9, 0))
                .orderDesc("New Order")
                .orderFee(100.0)
                .cart(newCart)
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        OrderDto result = orderService.save(newOrderDto);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.getOrderId(), "Order ID should be set");
        assertEquals("New Order", result.getOrderDesc(), "Order description should match");
        assertEquals(100.0, result.getOrderFee(), "Order fee should match");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Business Value: Validates order deletion from the system
     * Critical for: Order cancellation, administrative operations
     */
    @Test
    @DisplayName("deleteById() with valid ID should delete order")
    void testDeleteById_ValidId_DeletesOrder() {
        // Arrange
        Integer orderId = 1;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderRepository).delete(any(Order.class));

        // Act
        orderService.deleteById(orderId);

        // Assert
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).delete(any(Order.class));
    }
}
