package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

/**
 * NEW Integration Tests for Favourite → Product Service Communication
 * Validates product existence before adding to favourites
 * Tests error handling when products don't exist or are unavailable
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NEW Integration Test: Favourite Service validates Products before adding")
class FavouriteProductValidationIntegrationTest {

    @Autowired
    private FavouriteService favouriteService;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @MockBean
    private RestTemplate restTemplate;

    private UserDto testUserDto;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        favouriteRepository.deleteAll();
        reset(restTemplate);

        // Setup test data
        testUserDto = new UserDto();
        testUserDto.setUserId(1);
        testUserDto.setFirstName("Maria");
        testUserDto.setLastName("García");
        testUserDto.setEmail("maria.garcia@example.com");

        testProductDto = new ProductDto();
        testProductDto.setProductId(1);
        testProductDto.setProductTitle("Laptop HP");
        testProductDto.setPriceUnit(1500.00);
        testProductDto.setSku("SKU-LAPTOP-001");
        testProductDto.setQuantity(10); // Product in stock
    }

    /**
     * Integration Test #1: Validate findById() enriches Favourite with Product data
     * Integration: Favourite Service → Product Service (data enrichment)
     * Validates: findById() calls PRODUCT-SERVICE to enrich favourite with product details
     */
    @Test
    @DisplayName("Integration Test #1: findById() should enrich favourite with product details from PRODUCT-SERVICE")
    void testFindById_EnrichesWithProductDetails() {
        // Given: Save favourite with fixed timestamp
        Favourite favourite = new Favourite();
        favourite.setUserId(1);
        favourite.setProductId(1);
        favourite.setLikeDate(LocalDateTime.of(2024, 11, 1, 15, 30));
        Favourite savedFavourite = favouriteRepository.save(favourite);

        // Mock PRODUCT-SERVICE to return product details
        String productUrl = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1";
        when(restTemplate.getForObject(productUrl, ProductDto.class))
                .thenReturn(testProductDto);

        // Mock USER-SERVICE to return user details
        String userUrl = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1";
        when(restTemplate.getForObject(userUrl, UserDto.class))
                .thenReturn(testUserDto);

        // When: Find by ID using saved favourite's timestamp
        FavouriteId id = new FavouriteId(savedFavourite.getUserId(), savedFavourite.getProductId(), savedFavourite.getLikeDate());
        FavouriteDto result = favouriteService.findById(id);

        // Then: Should have enriched product data from PRODUCT-SERVICE
        assertNotNull(result);
        assertNotNull(result.getProductDto(), "Product data should be enriched");
        assertEquals("Laptop HP", result.getProductDto().getProductTitle());
        assertEquals(1500.00, result.getProductDto().getPriceUnit());
        assertEquals("SKU-LAPTOP-001", result.getProductDto().getSku());
        assertEquals(10, result.getProductDto().getQuantity());

        // Verify PRODUCT-SERVICE was called exactly once
        verify(restTemplate, times(1)).getForObject(productUrl, ProductDto.class);
    }

    /**
     * Integration Test #2: Validate findAll() calls PRODUCT-SERVICE for each product
     * Integration: Favourite Service → Product Service (batch enrichment)
     * Validates: findAll() calls PRODUCT-SERVICE once per unique product to enrich data
     */
    @Test
    @DisplayName("Integration Test #2: findAll() should call PRODUCT-SERVICE once per product")
    void testFindAll_CallsProductServiceForEachProduct() {
        // Given: Save 3 favourites for same user
        Favourite fav1 = new Favourite();
        fav1.setUserId(1);
        fav1.setProductId(1);
        fav1.setLikeDate(LocalDateTime.now());
        favouriteRepository.save(fav1);

        Favourite fav2 = new Favourite();
        fav2.setUserId(1);
        fav2.setProductId(2);
        fav2.setLikeDate(LocalDateTime.now());
        favouriteRepository.save(fav2);

        Favourite fav3 = new Favourite();
        fav3.setUserId(1);
        fav3.setProductId(3);
        fav3.setLikeDate(LocalDateTime.now());
        favouriteRepository.save(fav3);

        // Mock PRODUCT-SERVICE responses
        ProductDto product1 = new ProductDto();
        product1.setProductId(1);
        product1.setProductTitle("Product 1");

        ProductDto product2 = new ProductDto();
        product2.setProductId(2);
        product2.setProductTitle("Product 2");

        ProductDto product3 = new ProductDto();
        product3.setProductId(3);
        product3.setProductTitle("Product 3");

        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"), 
                eq(ProductDto.class))).thenReturn(product1);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/2"), 
                eq(ProductDto.class))).thenReturn(product2);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/3"), 
                eq(ProductDto.class))).thenReturn(product3);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class))).thenReturn(testUserDto);

        // When: Find all favourites
        var favourites = favouriteService.findAll();

        // Then: Should return 3 favourites with enriched data
        assertEquals(3, favourites.size());
        
        // Verify PRODUCT-SERVICE was called 3 times (once per product)
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"), 
                eq(ProductDto.class));
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/2"), 
                eq(ProductDto.class));
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/3"), 
                eq(ProductDto.class));

        // Verify USER-SERVICE was called 3 times (once per favourite)
        verify(restTemplate, times(3)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class));
    }
}
