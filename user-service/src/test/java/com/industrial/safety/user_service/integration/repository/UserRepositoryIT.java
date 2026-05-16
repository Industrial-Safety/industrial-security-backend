package com.industrial.safety.user_service.integration.repository;

import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("UserRepository — Pruebas de Integración con PostgreSQL")
class UserRepositoryIT {

    @Autowired
    UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(
                User.builder()
                        .name("María")
                        .lastName("García")
                        .email("maria.garcia@example.com")
                        .role("ROLE_ALUMNO")
                        .keycloakId("kc-001")
                        .isActive(true)
                        .mustChangePassword(false)
                        .createAccount(LocalDate.now())
                        .build()
        );

        user2 = userRepository.save(
                User.builder()
                        .name("Pedro")
                        .lastName("Martínez")
                        .email("pedro.martinez@example.com")
                        .role("ROLE_INSTRUCTOR")
                        .keycloakId("kc-002")
                        .isActive(true)
                        .mustChangePassword(true)
                        .createAccount(LocalDate.now())
                        .build()
        );
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // =========================================================
    //  findByEmail
    // =========================================================

    @Test
    @DisplayName("findByEmail: devuelve el usuario cuando el email existe")
    void findByEmail_found() {
        Optional<User> result = userRepository.findByEmail("maria.garcia@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("María");
        assertThat(result.get().getRole()).isEqualTo("ROLE_ALUMNO");
    }

    @Test
    @DisplayName("findByEmail: devuelve empty cuando el email no existe")
    void findByEmail_notFound() {
        Optional<User> result = userRepository.findByEmail("noexiste@example.com");

        assertThat(result).isEmpty();
    }

    // =========================================================
    //  findByKeycloakId
    // =========================================================

    @Test
    @DisplayName("findByKeycloakId: devuelve el usuario cuando el keycloakId existe")
    void findByKeycloakId_found() {
        Optional<User> result = userRepository.findByKeycloakId("kc-002");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("pedro.martinez@example.com");
        assertThat(result.get().getMustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("findByKeycloakId: devuelve empty cuando el keycloakId no existe")
    void findByKeycloakId_notFound() {
        Optional<User> result = userRepository.findByKeycloakId("kc-inexistente");

        assertThat(result).isEmpty();
    }

    // =========================================================
    //  findAll
    // =========================================================

    @Test
    @DisplayName("findAll: devuelve todos los usuarios guardados")
    void findAll_returnsAll() {
        assertThat(userRepository.findAll()).hasSize(2);
    }

    // =========================================================
    //  save / update
    // =========================================================

    @Test
    @DisplayName("save: persiste correctamente un nuevo usuario")
    void save_persistsNewUser() {
        User newUser = userRepository.save(
                User.builder()
                        .name("Laura")
                        .lastName("Sánchez")
                        .email("laura.sanchez@example.com")
                        .role("ROLE_ALUMNO")
                        .keycloakId("kc-003")
                        .isActive(true)
                        .mustChangePassword(false)
                        .createAccount(LocalDate.now())
                        .build()
        );

        assertThat(newUser.getId()).isNotNull();
        assertThat(userRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("save: actualiza un usuario existente")
    void save_updatesUser() {
        user1.setName("María Elena");
        userRepository.save(user1);

        Optional<User> updated = userRepository.findById(user1.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("María Elena");
    }

    // =========================================================
    //  deleteById
    // =========================================================

    @Test
    @DisplayName("deleteById: elimina el usuario correctamente")
    void deleteById_removesUser() {
        userRepository.deleteById(user1.getId());

        assertThat(userRepository.findById(user1.getId())).isEmpty();
        assertThat(userRepository.findAll()).hasSize(1);
    }
}
