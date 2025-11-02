package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

/**
 * Integration Tests for CartServiceImpl
 * Tests service communication with User microservice
 * Validates that Cart service correctly fetches user data from USER-SERVICE
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CartService Integration Tests - Order to User Communication")
class CartServiceImplIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @MockBean
    private RestTemplate restTemplate;

    private Cart testCart;
    private UserDto testUserDto;

    /**
     * Setup method executed before each test
     * Initializes test data and cleans database
     */
    @BeforeEach
    void setUp() {
        // Clean database
        cartRepository.deleteAll();

        // Reset mocks
        reset(restTemplate);

        // Setup test UserDto (simulating response from USER-SERVICE)
        testUserDto = new UserDto();
        testUserDto.setUserId(1);
        testUserDto.setFirstName("Juan");
        testUserDto.setLastName("Pérez");
        testUserDto.setEmail("juan.perez@example.com");
        testUserDto.setPhone("+57 300 123 4567");
        testUserDto.setImageUrl("https://example.com/avatar.jpg");

        // Setup test Cart
        testCart = new Cart();
        testCart.setUserId(1);
        testCart = cartRepository.save(testCart);
    }

    /**
     * Test 1: Verify Cart service fetches user data from USER-SERVICE when finding all carts
     * Integration: Order Service → User Service (GET /users/{userId})
     */
    @Test
    @DisplayName("Integration Test 1: findAll() should fetch user data from USER-SERVICE for each cart")
    void testFindAll_FetchesUserDataFromUserService() {
        // Given: Mock USER-SERVICE response
        String expectedUrl = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + testUserDto.getUserId();
        when(restTemplate.getForObject(expectedUrl, UserDto.class))
                .thenReturn(testUserDto);

        // When: Find all carts
        List<CartDto> result = cartService.findAll();

        // Then: Should call USER-SERVICE and populate user data
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        CartDto cartDto = result.get(0);
        assertNotNull(cartDto.getUserDto(), "User data should be fetched from USER-SERVICE");
        assertEquals(testUserDto.getUserId(), cartDto.getUserDto().getUserId());
        assertEquals(testUserDto.getFirstName(), cartDto.getUserDto().getFirstName());
        assertEquals(testUserDto.getLastName(), cartDto.getUserDto().getLastName());
        assertEquals(testUserDto.getEmail(), cartDto.getUserDto().getEmail());

        // Verify REST call was made to USER-SERVICE
        verify(restTemplate, times(1)).getForObject(expectedUrl, UserDto.class);
    }

    /**
     * Test 2: Verify Cart service fetches user data from USER-SERVICE when finding by ID
     * Integration: Order Service → User Service (GET /users/{userId})
     */
    @Test
    @DisplayName("Integration Test 2: findById() should fetch user data from USER-SERVICE")
    void testFindById_FetchesUserDataFromUserService() {
        // Given: Mock USER-SERVICE response
        String expectedUrl = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + testUserDto.getUserId();
        when(restTemplate.getForObject(expectedUrl, UserDto.class))
                .thenReturn(testUserDto);

        // When: Find cart by ID
        CartDto result = cartService.findById(testCart.getCartId());

        // Then: Should call USER-SERVICE and populate user data
        assertNotNull(result);
        assertNotNull(result.getUserDto(), "User data should be fetched from USER-SERVICE");
        assertEquals(testUserDto.getUserId(), result.getUserDto().getUserId());
        assertEquals(testUserDto.getFirstName(), result.getUserDto().getFirstName());
        assertEquals(testUserDto.getEmail(), result.getUserDto().getEmail());

        // Verify REST call was made to USER-SERVICE
        verify(restTemplate, times(1)).getForObject(expectedUrl, UserDto.class);
    }

    /**
     * Test 3: Verify Cart service handles USER-SERVICE unavailability gracefully
     * Integration: Order Service → User Service (error handling)
     */
    @Test
    @DisplayName("Integration Test 3: findById() should handle USER-SERVICE errors")
    void testFindById_HandlesUserServiceError() {
        // Given: USER-SERVICE is unavailable or returns error
        String expectedUrl = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + testUserDto.getUserId();
        when(restTemplate.getForObject(expectedUrl, UserDto.class))
                .thenThrow(new RuntimeException("USER-SERVICE unavailable"));

        // When & Then: Should propagate exception (no silent failures)
        assertThrows(RuntimeException.class, () -> {
            cartService.findById(testCart.getCartId());
        });

        // Verify REST call was attempted
        verify(restTemplate, times(1)).getForObject(expectedUrl, UserDto.class);
    }

    /**
     * Test 4: Verify Cart service throws exception when cart not found
     * Unit behavior test within integration context
     */
    @Test
    @DisplayName("Integration Test 4: findById() should throw CartNotFoundException for non-existent cart")
    void testFindById_ThrowsExceptionWhenCartNotFound() {
        // Given: Non-existent cart ID
        Integer nonExistentCartId = 99999;

        // When & Then: Should throw CartNotFoundException
        assertThrows(CartNotFoundException.class, () -> {
            cartService.findById(nonExistentCartId);
        });

        // Verify USER-SERVICE was never called (cart doesn't exist)
        verify(restTemplate, never()).getForObject(anyString(), eq(UserDto.class));
    }

    /**
     * Test 5: Verify save() creates cart without calling USER-SERVICE
     * Validates that save operation doesn't trigger unnecessary external calls
     */
    @Test
    @DisplayName("Integration Test 5: save() should create cart without calling USER-SERVICE")
    void testSave_DoesNotCallUserService() {
        // Given: New cart DTO
        CartDto newCartDto = new CartDto();
        newCartDto.setUserDto(testUserDto);

        // When: Save cart
        CartDto result = cartService.save(newCartDto);

        // Then: Should save cart without calling USER-SERVICE
        assertNotNull(result);
        assertNotNull(result.getCartId());

        // Verify USER-SERVICE was NOT called during save
        verify(restTemplate, never()).getForObject(anyString(), eq(UserDto.class));
        
        // Verify cart exists in database
        Optional<Cart> savedCart = cartRepository.findById(result.getCartId());
        assertTrue(savedCart.isPresent());
    }
}
