package com.selimhorri.app.service.impl;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link OrderItemServiceImpl}.
 * Tests the integration between Order Item Service, Product Service, and Order Service.
 * Uses @SpringBootTest for full Spring context and @MockBean for external HTTP calls.
 */
@SpringBootTest
@ActiveProfiles("test")
public class OrderItemServiceImplIntegrationTest {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockBean
    private RestTemplate restTemplate;

    private ProductDto testProductDto;
    private OrderDto testOrderDto;

    /**
     * Sets up test data before each test.
     * Cleans the database and resets RestTemplate mock to ensure test isolation.
     */
    @BeforeEach
    void setUp() {
        // Clear all order items from the database for test isolation
        orderItemRepository.deleteAll();

        // Reset the RestTemplate mock to clear any previous interactions
        reset(restTemplate);

        // Prepare test ProductDto
        testProductDto = new ProductDto();
        testProductDto.setProductId(1);
        testProductDto.setProductTitle("Test Product");
        testProductDto.setImageUrl("http://test.com/product.jpg");
        testProductDto.setSku("SKU123");
        testProductDto.setPriceUnit(50.0);
        testProductDto.setQuantity(100);

        // Prepare test OrderDto
        testOrderDto = new OrderDto();
        testOrderDto.setOrderId(1);
        testOrderDto.setOrderDate(LocalDateTime.now());
        testOrderDto.setOrderDesc("Test Order");
        testOrderDto.setOrderFee(50.0);
    }

    /**
     * Tests that findAll() retrieves all order items and enriches them with product and order data
     * by calling external Product and Order services via RestTemplate.
     * 
     * Business Value: Validates complete order item list with product details (name, price)
     * and order information (date, total) for inventory and shipping management.
     */
    @Test
    void testFindAll_CallsProductAndOrderServices_ReturnsEnrichedOrderItems() {
        // Given: An order item exists in the database
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(1);
        orderItem.setOrderId(1);
        orderItem.setOrderedQuantity(5);
        orderItemRepository.save(orderItem);

        // Mock Product Service response
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"),
                eq(ProductDto.class)))
                .thenReturn(testProductDto);

        // Mock Order Service response
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
                eq(OrderDto.class)))
                .thenReturn(testOrderDto);

        // When: We fetch all order items
        List<OrderItemDto> result = orderItemService.findAll();

        // Then: The result should contain enriched order item data
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        
        OrderItemDto orderItemDto = result.get(0);
        assertThat(orderItemDto.getProductId()).isEqualTo(1);
        assertThat(orderItemDto.getOrderId()).isEqualTo(1);
        assertThat(orderItemDto.getOrderedQuantity()).isEqualTo(5);
        
        // Verify product enrichment
        assertThat(orderItemDto.getProductDto()).isNotNull();
        assertThat(orderItemDto.getProductDto().getProductTitle()).isEqualTo("Test Product");
        
        // Verify order enrichment
        assertThat(orderItemDto.getOrderDto()).isNotNull();
        assertThat(orderItemDto.getOrderDto().getOrderDesc()).isEqualTo("Test Order");

        // Verify RestTemplate was called for both Product and Order services
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"),
                eq(ProductDto.class));
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
                eq(OrderDto.class));
    }

    /**
     * Tests that save() creates a new order item without making external HTTP calls.
     * 
     * Business Value: Validates order item creation performance - write operations should not
     * require external service calls, reducing latency for order processing.
     */
    @Test
    void testSave_CreatesOrderItem_WithoutExternalCalls() {
        // Given: A new order item DTO to save
        OrderItemDto newOrderItemDto = new OrderItemDto();
        newOrderItemDto.setProductId(2);
        newOrderItemDto.setOrderId(2);
        newOrderItemDto.setOrderedQuantity(3);

        // When: We save the order item
        OrderItemDto result = orderItemService.save(newOrderItemDto);

        // Then: The order item should be created successfully
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(2);
        assertThat(result.getOrderId()).isEqualTo(2);
        assertThat(result.getOrderedQuantity()).isEqualTo(3);

        // Verify the order item is persisted in the database
        OrderItemId orderItemId = new OrderItemId(2, 2);
        boolean exists = orderItemRepository.existsById(orderItemId);
        assertThat(exists).isTrue();

        // Verify NO external calls were made during save
        verify(restTemplate, never()).getForObject(anyString(), eq(ProductDto.class));
        verify(restTemplate, never()).getForObject(anyString(), eq(OrderDto.class));
    }
}
