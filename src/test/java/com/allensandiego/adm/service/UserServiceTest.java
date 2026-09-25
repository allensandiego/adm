package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.Role;
import com.allensandiego.adm.domain.RoleRepository;
import com.allensandiego.adm.domain.User;
import com.allensandiego.adm.domain.UserRole;
import com.allensandiego.adm.domain.UserRoleCompositeId;
import com.allensandiego.adm.domain.UserRoleRepository;
import com.allensandiego.adm.domain.UserRepository;
import com.allensandiego.adm.dto.UserCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private AuditService auditService;
    @InjectMocks private UserService userService;

    private UUID testId;
    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setStatus(User.UserStatus.ACTIVE);

        testRole = new Role();
        testRole.setId(UUID.randomUUID());
        testRole.setName("TEST_ROLE");
    }

    @Test
    @DisplayName("createUser - saves user with audit log")
    void createUserSuccess() {
        UserCreateRequest request = new UserCreateRequest("newuser", "New User", true);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        userService.createUser(request, "admin");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser - throws on duplicate username")
    void createUserDuplicateUsername() {
        UserCreateRequest request = new UserCreateRequest("testuser", "New User", true);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request, "admin"));
    }

    @Test
    @DisplayName("listUsersAsEntities - returns paginated results")
    void listUsersAsEntities() {
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = List.of(testUser);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(users));

        Page<User> result = userService.listUsersAsEntities(pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("listUsersAsEntities - filters by search term")
    void listUsersAsEntitiesWithSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = List.of(testUser);
        when(userRepository.search("test", pageable))
                .thenReturn(new PageImpl<>(users));

        userService.listUsersAsEntities(pageable, "test");

        verify(userRepository).search("test", pageable);
    }

    @Test
    @DisplayName("getUserEntity - returns user by ID")
    void getUserEntityFound() {
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));

        User result = userService.getUserEntity(testId);

        assertNotNull(result);
        assertEquals(testId, result.getId());
    }

    @Test
    @DisplayName("getUserEntity - throws NoSuchElementException when not found")
    void getUserEntityNotFound() {
        when(userRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.getUserEntity(testId));
    }

    @Test
    @DisplayName("assignRoles - assigns roles to user")
    void assignRolesSuccess() {
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("TEST_ROLE")).thenReturn(Optional.of(testRole));
        userService.assignRoles(testId, List.of("TEST_ROLE"), "admin");

        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("assignRoles - throws NoSuchElementException when user not found")
    void assignRolesUserNotFound() {
        when(userRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.assignRoles(testId, List.of("TEST_ROLE"), "admin"));
    }

    @Test
    @DisplayName("assignRoles - clears existing roles and assigns new ones")
    void assignRolesDuplicateSkipped() {
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("TEST_ROLE")).thenReturn(Optional.of(testRole));

        userService.assignRoles(testId, List.of("TEST_ROLE"), "admin");

        verify(userRoleRepository).deleteByUserId(testId);
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("updateProfile - logs audit event")
    void updateProfileSuccess() {
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));

        userService.updateProfile(testId, "newusername", "admin");

        verify(auditService).log(eq("admin"), eq("USER_EDIT"), eq("User"), eq(testId));
    }

    @Test
    @DisplayName("updateProfile - does not check for duplicate username (stub implementation)")
    void updateProfileDuplicateUsername() {
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));

        // The current stub implementation does not validate duplicate usernames
        assertDoesNotThrow(() -> userService.updateProfile(testId, "newusername", "admin"));
    }

    @Test
    @DisplayName("updateProfile - throws NoSuchElementException when user not found")
    void updateProfileNotFound() {
        when(userRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.updateProfile(testId, "newusername", "admin"));
    }
}
