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

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;

/**
 * Unit Tests for UserServiceImpl
 * Tests user management operations in isolation
 * Following DevOps best practices: Fast, Focused, Independent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;
    private Credential testCredential;
    private CredentialDto testCredentialDto;

    @BeforeEach
    void setUp() {
        // Setup test credentials
        testCredential = Credential.builder()
                .credentialId(1)
                .username("testuser")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testCredentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        // Setup test user
        testUser = User.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("http://example.com/john.jpg")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .credential(testCredential)
                .build();

        testUserDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("http://example.com/john.jpg")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .credentialDto(testCredentialDto)
                .build();
    }

    /**
     * Test 1: Verify findAll returns all users
     * Business Value: Essential for user management dashboard
     */
    @Test
    @DisplayName("Should return all users when findAll is called")
    void testFindAll_ReturnsAllUsers() {
        // Arrange
        User user2 = User.builder()
                .userId(2)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("+9876543210")
                .credential(testCredential)
                .build();

        List<User> expectedUsers = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // Act
        List<UserDto> result = userService.findAll();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 users");
        assertEquals("John", result.get(0).getFirstName(), "First user should be John");
        assertEquals("Jane", result.get(1).getFirstName(), "Second user should be Jane");
        
        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test 2: Verify findById returns correct user
     * Business Value: Critical for user profile and authentication
     */
    @Test
    @DisplayName("Should return user when valid ID is provided")
    void testFindById_ValidId_ReturnsUser() {
        // Arrange
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.findById(userId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(userId, result.getUserId(), "User ID should match");
        assertEquals("John", result.getFirstName(), "First name should match");
        assertEquals("Doe", result.getLastName(), "Last name should match");
        assertEquals("john.doe@example.com", result.getEmail(), "Email should match");
        assertEquals("testuser", result.getCredentialDto().getUsername(), "Username should match");
        
        verify(userRepository, times(1)).findById(userId);
    }

    /**
     * Test 3: Verify findById throws exception for invalid ID
     * Business Value: Ensures proper error handling
     */
    @Test
    @DisplayName("Should throw UserObjectNotFoundException when user ID does not exist")
    void testFindById_InvalidId_ThrowsException() {
        // Arrange
        Integer invalidId = 999;
        when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        UserObjectNotFoundException exception = assertThrows(
            UserObjectNotFoundException.class,
            () -> userService.findById(invalidId),
            "Should throw UserObjectNotFoundException"
        );

        assertTrue(
            exception.getMessage().contains("not found"),
            "Exception message should indicate user not found"
        );
        
        verify(userRepository, times(1)).findById(invalidId);
    }

    /**
     * Test 4: Verify save creates new user successfully
     * Business Value: Essential for user registration
     */
    @Test
    @DisplayName("Should save and return new user")
    void testSave_ValidUser_ReturnsCreatedUser() {
        // Arrange
        CredentialDto newCredentialDto = CredentialDto.builder()
                .username("newuser")
                .password("$2a$10$encryptedPassword")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .build();

        UserDto newUserDto = UserDto.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .phone("+1122334455")
                .credentialDto(newCredentialDto)
                .build();

        Credential newCredential = Credential.builder()
                .credentialId(2)
                .username("newuser")
                .password("$2a$10$encryptedPassword")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .build();

        User savedUser = User.builder()
                .userId(3)
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .phone("+1122334455")
                .credential(newCredential)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserDto result = userService.save(newUserDto);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.getUserId(), "User ID should be set");
        assertEquals("Alice", result.getFirstName(), "First name should match");
        assertEquals("Johnson", result.getLastName(), "Last name should match");
        assertEquals("alice.johnson@example.com", result.getEmail(), "Email should match");
        
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Test 5: Verify deleteById removes user successfully
     * Business Value: Critical for user account management
     */
    @Test
    @DisplayName("Should delete user when valid ID is provided")
    void testDeleteById_ValidId_DeletesUser() {
        // Arrange
        Integer userId = 1;
        doNothing().when(userRepository).deleteById(userId);

        // Act
        assertDoesNotThrow(
            () -> userService.deleteById(userId),
            "Should not throw exception when deleting valid user"
        );

        // Assert
        verify(userRepository, times(1)).deleteById(userId);
    }

    /**
     * Additional Test: Verify findByUsername returns correct user
     * Business Value: Essential for authentication and login
     */
    @Test
    @DisplayName("Should return user when valid username is provided")
    void testFindByUsername_ValidUsername_ReturnsUser() {
        // Arrange
        String username = "testuser";
        when(userRepository.findByCredentialUsername(username))
                .thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.findByUsername(username);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(username, result.getCredentialDto().getUsername(), "Username should match");
        assertEquals("John", result.getFirstName(), "First name should match");
        assertEquals("john.doe@example.com", result.getEmail(), "Email should match");
        
        verify(userRepository, times(1)).findByCredentialUsername(username);
    }

    /**
     * Additional Test: Verify findByUsername throws exception for invalid username
     * Business Value: Ensures proper error handling during authentication
     */
    @Test
    @DisplayName("Should throw exception when username does not exist")
    void testFindByUsername_InvalidUsername_ThrowsException() {
        // Arrange
        String invalidUsername = "nonexistent";
        when(userRepository.findByCredentialUsername(invalidUsername))
                .thenReturn(Optional.empty());

        // Act & Assert
        UserObjectNotFoundException exception = assertThrows(
            UserObjectNotFoundException.class,
            () -> userService.findByUsername(invalidUsername),
            "Should throw UserObjectNotFoundException"
        );

        assertTrue(
            exception.getMessage().contains("not found"),
            "Exception message should indicate user not found"
        );
        
        verify(userRepository, times(1)).findByCredentialUsername(invalidUsername);
    }

}
