package com.toolrent.controllers;

import com.toolrent.entities.UserEntity;
import com.toolrent.entities.UserRole;
import com.toolrent.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Registra un nuevo usuario", description = "Crea un nuevo usuario con nombre, contraseña, y rol")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")  // Solo ADMIN registra
    public ResponseEntity<UserEntity> register(@RequestParam String username, @RequestParam String password, @RequestParam UserRole role) {
        UserEntity user = userService.registerUser(username, password, role);
        return ResponseEntity.ok(user);
    }


    @Operation(summary = "Asigna rol al usuario", description = "Actualiza el rol del usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role assigned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignRole(@PathVariable Long id, @RequestParam UserRole role) {
        userService.assignRole(id, role);
        return ResponseEntity.ok().build();
    }
}