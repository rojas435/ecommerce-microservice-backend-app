package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;

/**
 * Unit Tests for ProductServiceImpl
 * Tests individual components in isolation using mocks
 * Following DevOps best practices: Fast, Focused, Independent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDto testProductDto;
    private Category testCategory;
    private CategoryDto testCategoryDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        testCategory = Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("http://example.com/electronics.jpg")
                .build();

        testCategoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("http://example.com/electronics.jpg")
                .build();

        testProduct = Product.builder()
                .productId(1)
                .productTitle("Laptop")
                .imageUrl("http://example.com/laptop.jpg")
                .sku("LAP-001")
                .priceUnit(999.99)
                .quantity(10)
                .category(testCategory)
                .build();

        testProductDto = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop")
                .imageUrl("http://example.com/laptop.jpg")
                .sku("LAP-001")
                .priceUnit(999.99)
                .quantity(10)
                .categoryDto(testCategoryDto)
                .build();
    }

    /**
     * Test 1: Verify findAll returns all products
     * Business Value: Ensures catalog listing functionality works
     */
    @Test
    @DisplayName("Should return all products when findAll is called")
    void testFindAll_ReturnsAllProducts() {
        // Arrange
        Product product2 = Product.builder()
                .productId(2)
                .productTitle("Mouse")
                .sku("MOU-001")
                .priceUnit(29.99)
                .quantity(50)
                .category(testCategory)
                .build();

        List<Product> expectedProducts = Arrays.asList(testProduct, product2);
        when(productRepository.findAll()).thenReturn(expectedProducts);

        // Act
        List<ProductDto> result = productService.findAll();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 products");
        assertEquals("Laptop", result.get(0).getProductTitle(), "First product should be Laptop");
        assertEquals("Mouse", result.get(1).getProductTitle(), "Second product should be Mouse");
        
        // Verify repository interaction
        verify(productRepository, times(1)).findAll();
    }

    /**
     * Test 2: Verify findById returns correct product
     * Business Value: Critical for product detail page functionality
     */
    @Test
    @DisplayName("Should return product when valid ID is provided")
    void testFindById_ValidId_ReturnsProduct() {
        // Arrange
        Integer productId = 1;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // Act
        ProductDto result = productService.findById(productId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(productId, result.getProductId(), "Product ID should match");
        assertEquals("Laptop", result.getProductTitle(), "Product title should match");
        assertEquals("LAP-001", result.getSku(), "SKU should match");
        assertEquals(999.99, result.getPriceUnit(), "Price should match");
        assertEquals(10, result.getQuantity(), "Quantity should match");
        
        // Verify repository interaction
        verify(productRepository, times(1)).findById(productId);
    }

    /**
     * Test 3: Verify findById throws exception for invalid ID
     * Business Value: Ensures proper error handling for non-existent products
     */
    @Test
    @DisplayName("Should throw ProductNotFoundException when product ID does not exist")
    void testFindById_InvalidId_ThrowsException() {
        // Arrange
        Integer invalidId = 999;
        when(productRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(
            ProductNotFoundException.class,
            () -> productService.findById(invalidId),
            "Should throw ProductNotFoundException"
        );

        assertTrue(
            exception.getMessage().contains("not found"),
            "Exception message should indicate product not found"
        );
        
        // Verify repository interaction
        verify(productRepository, times(1)).findById(invalidId);
    }

    /**
     * Test 4: Verify save creates new product successfully
     * Business Value: Essential for inventory management and product creation
     */
    @Test
    @DisplayName("Should save and return new product")
    void testSave_ValidProduct_ReturnsCreatedProduct() {
        // Arrange
        ProductDto newProductDto = ProductDto.builder()
                .productTitle("Keyboard")
                .sku("KEY-001")
                .priceUnit(79.99)
                .quantity(25)
                .categoryDto(testCategoryDto)
                .build();

        Product savedProduct = Product.builder()
                .productId(3)
                .productTitle("Keyboard")
                .sku("KEY-001")
                .priceUnit(79.99)
                .quantity(25)
                .category(testCategory)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // Act
        ProductDto result = productService.save(newProductDto);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.getProductId(), "Product ID should be set");
        assertEquals("Keyboard", result.getProductTitle(), "Product title should match");
        assertEquals("KEY-001", result.getSku(), "SKU should match");
        assertEquals(79.99, result.getPriceUnit(), "Price should match");
        assertEquals(25, result.getQuantity(), "Quantity should match");
        
        // Verify repository interaction
        verify(productRepository, times(1)).save(any(Product.class));
    }

    /**
     * Test 5: Verify deleteById removes product successfully
     * Business Value: Critical for inventory management and product lifecycle
     */
    @Test
    @DisplayName("Should delete product when valid ID is provided")
    void testDeleteById_ValidId_DeletesProduct() {
        // Arrange
        Integer productId = 1;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).delete(any(Product.class));

        // Act
        assertDoesNotThrow(
            () -> productService.deleteById(productId),
            "Should not throw exception when deleting valid product"
        );

        // Assert
        // Verify findById was called (part of deleteById implementation)
        verify(productRepository, times(1)).findById(productId);
        // Verify delete was called
        verify(productRepository, times(1)).delete(any(Product.class));
    }

    /**
     * Additional Test: Verify deleteById throws exception for invalid ID
     * Business Value: Ensures proper error handling when trying to delete non-existent product
     */
    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void testDeleteById_InvalidId_ThrowsException() {
        // Arrange
        Integer invalidId = 999;
        when(productRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            ProductNotFoundException.class,
            () -> productService.deleteById(invalidId),
            "Should throw ProductNotFoundException when deleting non-existent product"
        );

        // Verify repository interactions
        verify(productRepository, times(1)).findById(invalidId);
        verify(productRepository, never()).delete(any(Product.class));
    }

    /**
     * Additional Test: Verify update modifies product successfully
     * Business Value: Essential for inventory updates and price changes
     */
    @Test
    @DisplayName("Should update existing product")
    void testUpdate_ValidProduct_ReturnsUpdatedProduct() {
        // Arrange
        ProductDto updatedDto = ProductDto.builder()
                .productId(1)
                .productTitle("Updated Laptop")
                .sku("LAP-001")
                .priceUnit(899.99)  // Price reduced
                .quantity(15)       // Quantity increased
                .categoryDto(testCategoryDto)
                .build();

        Product updatedProduct = Product.builder()
                .productId(1)
                .productTitle("Updated Laptop")
                .sku("LAP-001")
                .priceUnit(899.99)
                .quantity(15)
                .category(testCategory)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // Act
        ProductDto result = productService.update(updatedDto);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getProductId(), "Product ID should remain the same");
        assertEquals("Updated Laptop", result.getProductTitle(), "Title should be updated");
        assertEquals(899.99, result.getPriceUnit(), "Price should be updated");
        assertEquals(15, result.getQuantity(), "Quantity should be updated");

        // Verify repository interaction
        verify(productRepository, times(1)).save(any(Product.class));
    }

}
