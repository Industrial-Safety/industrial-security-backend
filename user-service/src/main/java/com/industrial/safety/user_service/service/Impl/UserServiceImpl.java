package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.dto.UserCreationResult;
import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.dto.UserUpdateRequest;
import com.industrial.safety.user_service.exception.ResourceNotFoundException;
import com.industrial.safety.user_service.mapper.UserMapper;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.UserRepository;
import com.industrial.safety.user_service.service.KeycloakService;
import com.industrial.safety.user_service.service.QrService;
import com.industrial.safety.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final QrService qrService;
    private final KeycloakService keycloakService;

    @Override
    public UserCreationResult createUser(UserRequest userRequest) {
        var userExistente = userRepository.findByEmail(userRequest.getEmail());
        if (userExistente.isPresent()) {
            User existing = userExistente.get();
            // Sincronizar keycloakId si el frontend lo provee y difiere del almacenado
            // (ocurre cuando Keycloak fue reseteado y el usuario fue recreado con nuevo UUID)
            String incomingKeycloakId = userRequest.getKeycloakId();
            if (incomingKeycloakId != null && !incomingKeycloakId.isBlank()
                    && !incomingKeycloakId.equals(existing.getKeycloakId())) {
                System.out.println("INFO [createUser] Sincronizando keycloakId: " + existing.getKeycloakId() + " -> " + incomingKeycloakId);
                existing.setKeycloakId(incomingKeycloakId);
                userRepository.save(existing);
            }
            System.out.println("El usuario ya estaba registrado en DB. Retornando existente...");
            return new UserCreationResult(userMapper.toUserResponse(existing), false);
        }

        String keycloakId;
        // If the caller explicitly sets mustChangePassword (e.g. registerStudent sets it to false),
        // use that value; otherwise fall back to detecting admin creation from the password.
        boolean createdByAdmin = userRequest.getMustChangePassword() != null
                ? userRequest.getMustChangePassword()
                : !userRequest.getPassword().equals("oauth_user_password");

        // Si el frontend ya provee keycloakId, el usuario existe en Keycloak — usar directo, sin llamar Admin API
        if (userRequest.getKeycloakId() != null && !userRequest.getKeycloakId().isBlank()) {
            keycloakId = userRequest.getKeycloakId();
            createdByAdmin = false;
        } else {
            try {
                keycloakId = keycloakService.createUser(userRequest);
            } catch (KeycloakServiceImpl.UserAlreadyExistsInKeycloakException e) {
                System.out.println("Usuario ya existe en Keycloak (OAuth previo). Obteniendo ID y asignando rol...");
                keycloakId = keycloakService.getUserIdByEmail(userRequest.getEmail());
                keycloakService.assignRole(keycloakId, userRequest.getRole());
                createdByAdmin = false;
            }
        }

        User user = userMapper.toUser(userRequest);
        user.setRole(userRequest.getRole());
        user.setIsActive(true);
        user.setCreateAccount(LocalDate.now());
        user.setKeycloakId(keycloakId);
        user.setMustChangePassword(createdByAdmin);
        user.setQrCodeUrl(qrService.generateAndUploadQr(
                keycloakId,
                userRequest.getName() + " " + userRequest.getLastName(),
                userRequest.getEmail(),
                userRequest.getRole()
        ));
        User nUser = userRepository.save(user);
        return new UserCreationResult(userMapper.toUserResponse(nUser), true);
    }

    @Override
    public List<UserResponse> toListUser() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No existe el id ", "id", id)
        );
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("No existe el email ", "email", email)
        );
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(String id, UserRequest userRequest) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No existe el id ", "id", id)
        );
        userMapper.updateUserFromRequest(userRequest, user);
        User userUpdate = userRepository.save(user);
        return userMapper.toUserResponse(userUpdate);
    }

    @Override
    public UserResponse updateUserAdmin(String id, UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No existe el id ", "id", id)
        );
        userMapper.updateUserFromRequestAdmin(userUpdateRequest, user);
        User userUpdate = userRepository.save(user);
        return userMapper.toUserResponse(userUpdate);
    }

    @Override
    public void changePassword(String keycloakId, String email, String newPassword) {
        keycloakService.updatePassword(keycloakId, newPassword);
        // Busca por keycloakId; si no encuentra (ID desincronizado en DB), cae a email
        boolean updated = userRepository.findByKeycloakId(keycloakId)
                .map(user -> { user.setMustChangePassword(false); userRepository.save(user); return true; })
                .orElse(false);
        if (!updated && email != null && !email.isBlank()) {
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setMustChangePassword(false);
                userRepository.save(user);
            });
        }
    }
}