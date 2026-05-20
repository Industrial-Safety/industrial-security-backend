package com.industrial.safety.user_service.controller;

import com.industrial.safety.user_service.dto.UserCreationResult;
import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.dto.UserUpdateRequest;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController
{
    private final UserService userService;
    private final KeycloakService keycloakService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerStudent(@Valid @RequestBody UserRequest userRequest) {
        userRequest.setRole("ROLE_ALUMNO");
        // Solo forzar false si no viene ya seteado (OAuth puede enviar true para forzar cambio de contraseña)
        if (userRequest.getMustChangePassword() == null) {
            userRequest.setMustChangePassword(false);
        }
        UserCreationResult result = userService.createUser(userRequest);
        HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.user());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest userRequest) {
        return userService.createUserAdmin(userRequest);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<UserResponse> toggleStatus(@PathVariable String id) {
        return ResponseEntity.ok(userService.toggleStatus(id));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserResponse> getAllUsers(@RequestParam(required = false) String role) {
        List<UserResponse> users = userService.toListUser();
        if (role == null || role.isBlank()) {
            return users;
        }
        String wanted = normalizeRole(role);
        return users.stream()
                .filter(u -> u.getRole() != null && normalizeRole(u.getRole()).equals(wanted))
                .toList();
    }

    private String normalizeRole(String role) {
        String r = role.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r.substring(5) : r;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @GetMapping("/by-email")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUserByEmail(@RequestParam String email) {
        return userService.getUserByEmail(email);
    }

    @GetMapping("/by-dni")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUserByDni(@RequestParam String dni) {
        return userService.getUserByDni(dni);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UserResponse updateUser(@PathVariable String id, @Valid @RequestBody UserRequest userRequest) {
        return userService.updateUser(id, userRequest);
    }

    @PutMapping("/admin/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UserResponse updateUserAdmin(@PathVariable String id, @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        return userService.updateUserAdmin(id, userUpdateRequest);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String newPassword = request.get("newPassword");
        if (userId == null || newPassword == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Faltan datos obligatorios (userId o newPassword)"));
        }
        String email = request.get("email");
        try {
            userService.changePassword(userId, email, newPassword);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Contrasena actualizada con exito"
            ));
        } catch (Exception e) {
            System.err.println("ERROR [changePassword] userId=" + userId + " | " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", "error",
                            "message", "La nueva contrasena no cumple con las politicas de seguridad.",
                            "debug", e.getClass().getSimpleName() + ": " + e.getMessage()
                    ));
        }
    }
}