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
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

/**
 * Integration Tests for FavouriteServiceImpl
 * Tests service communication with User and Product microservices
 * Uses @MockBean to mock RestTemplate calls to external services
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("FavouriteService Integration Tests")
class FavouriteServiceImplIntegrationTest {

    @Autowired
    private FavouriteService favouriteService;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @MockBean
    private RestTemplate restTemplate;

    private UserDto testUserDto;
    private ProductDto testProductDto;

    /**
     * Setup method executed before each test
     * Initializes test data and cleans database
     */
    @BeforeEach
    void setUp() {
        // Clean database
        favouriteRepository.deleteAll();

        // Reset mocks
        reset(restTemplate);

        // Setup test UserDto (no builder available, use constructor/setters)
        testUserDto = new UserDto();
        testUserDto.setUserId(1);
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@example.com");

        // Setup test ProductDto (no builder available, use constructor/setters)
        testProductDto = new ProductDto();
        testProductDto.setProductId(1);
        testProductDto.setProductTitle("Test Product");
        testProductDto.setPriceUnit(99.99);
        testProductDto.setSku("SKU-001");
    }

    /**
     * Test findAll() method
     * Validates that the service calls User and Product services and enriches favourites
     * 
     * Expected: 1 favourite returned with enriched user and product data
     */
    @Test
    @DisplayName("findAll() should call User and Product services and return enriched favourites")
    void testFindAll_CallsUserAndProductServices_ReturnsEnrichedFavourites() throws Exception {
        // Arrange: Save a favourite to database
        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 15, 10, 0);
        Favourite favourite = new Favourite();
        favourite.setUserId(1);
        favourite.setProductId(1);
        favourite.setLikeDate(likeDate);
        favouriteRepository.save(favourite);

        // Mock RestTemplate calls
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class)))
            .thenReturn(testUserDto);

        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"), 
                eq(ProductDto.class)))
            .thenReturn(testProductDto);

        // Act: Call service method
        List<FavouriteDto> favourites = favouriteService.findAll();

        // Assert: Verify results
        assertNotNull(favourites, "Favourites list should not be null");
        assertEquals(1, favourites.size(), "Should return 1 favourite");

        FavouriteDto favouriteDto = favourites.get(0);
        assertEquals(1, favouriteDto.getUserId(), "User ID should match");
        assertEquals(1, favouriteDto.getProductId(), "Product ID should match");

        // Verify external service calls were made
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class));
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"), 
                eq(ProductDto.class));
    }

    /**
     * Test findById() method
     * Validates that the service calls User and Product services for a single favourite
     * 
     * Expected: Single favourite returned with enriched data from external services
     */
    @Test
    @DisplayName("findById() should call external services and return enriched favourite")
    void testFindById_CallsExternalServices_ReturnsEnrichedFavourite() {
        // Arrange: Save a favourite to database
        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 15, 10, 0);
        Favourite favourite = new Favourite();
        favourite.setUserId(1);
        favourite.setProductId(1);
        favourite.setLikeDate(likeDate);
        favouriteRepository.save(favourite);

        FavouriteId favouriteId = new FavouriteId();
        favouriteId.setUserId(1);
        favouriteId.setProductId(1);
        favouriteId.setLikeDate(likeDate);

        // Mock RestTemplate calls
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class)))
            .thenReturn(testUserDto);

        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"), 
                eq(ProductDto.class)))
            .thenReturn(testProductDto);

        // Act: Call service method
        FavouriteDto result = favouriteService.findById(favouriteId);

        // Assert: Verify results
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getUserId(), "User ID should match");
        assertEquals(1, result.getProductId(), "Product ID should match");

        // Verify external service calls were made
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class));
        verify(restTemplate, times(1)).getForObject(
                eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/1"), 
                eq(ProductDto.class));
    }

    /**
     * Test save() method
     * Validates that save operation does NOT call external services (performance optimization)
     * 
     * Expected: Favourite saved successfully without external HTTP calls
     */
    @Test
    @DisplayName("save() should create favourite without calling external services")
    void testSave_CreatesFavourite_WithoutExternalCalls() {
        // Arrange: Create new FavouriteDto
        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 20, 14, 30);
        FavouriteDto newFavouriteDto = new FavouriteDto();
        newFavouriteDto.setUserId(2);
        newFavouriteDto.setProductId(3);
        newFavouriteDto.setLikeDate(likeDate);

        // Act: Save favourite
        FavouriteDto result = favouriteService.save(newFavouriteDto);

        // Assert: Verify saved
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.getUserId(), "User ID should match");
        assertEquals(3, result.getProductId(), "Product ID should match");
        assertNotNull(result.getLikeDate(), "Like date should not be null");

        // Verify NO external service calls were made (performance optimization)
        verify(restTemplate, never()).getForObject(anyString(), any());

        // Verify database persistence
        FavouriteId savedId = new FavouriteId();
        savedId.setUserId(2);
        savedId.setProductId(3);
        savedId.setLikeDate(likeDate);
        
        assertTrue(favouriteRepository.existsById(savedId), "Favourite should exist in database");
    }
}
