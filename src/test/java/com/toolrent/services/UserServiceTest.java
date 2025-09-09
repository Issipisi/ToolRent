package com.toolrent.services;

import com.toolrent.entities.UserEntity;
import com.toolrent.entities.UserRole;
import com.toolrent.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // CASO: Registro exitoso
    @Test
    void whenRegisterUser_withValidData_thenSuccess() {
        // Given
        String username = "newuser";
        String password = "pass123";
        UserRole role = UserRole.EMPLOYEE;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        UserEntity savedUser = new UserEntity();
        savedUser.setId(1L);
        savedUser.setUsername(username);
        savedUser.setPassword(password);
        savedUser.setRole(role);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // When
        UserEntity result = userService.registerUser(username, password, role);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getRole()).isEqualTo(role);
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    // CASO: Username ya existe
    @Test
    void whenRegisterUser_withExistingUsername_thenThrowsException() {
        // Given
        String username = "existinguser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new UserEntity()));

        // When / Then
        assertThatThrownBy(() -> userService.registerUser(username, "pass", UserRole.EMPLOYEE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    // CASO: Username vacío
    @Test
    void whenRegisterUser_withEmptyUsername_thenThrowsException() {
        assertThatThrownBy(() -> userService.registerUser("", "pass", UserRole.EMPLOYEE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username must not be empty"); // Porque findByUsername("") puede devolver algo
    }

    // CASO: Asignar rol exitosamente
    @Test
    void whenAssignRole_withValidId_thenSuccess() {
        // Given
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(UserRole.EMPLOYEE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // When
        userService.assignRole(userId, UserRole.ADMIN);

        // Then
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository, times(1)).save(user);
    }

    // CASO: Usuario no existe al asignar rol
    @Test
    void whenAssignRole_withInvalidId_thenThrowsException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.assignRole(userId, UserRole.ADMIN))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    // CASO: ID negativo al asignar rol
    @Test
    void whenAssignRole_withNegativeId_thenThrowsException() {
        Long negativeId = -1L;
        when(userRepository.findById(negativeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.assignRole(negativeId, UserRole.ADMIN))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // CASO: Rol nulo al registrar
    @Test
    void whenRegisterUser_withNullRole_thenStillSaves() {
        String username = "userNullRole";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        UserEntity saved = new UserEntity();
        saved.setUsername(username);
        saved.setRole(null);
        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

        UserEntity result = userService.registerUser(username, "pass", null);

        assertThat(result.getRole()).isNull();
    }
}