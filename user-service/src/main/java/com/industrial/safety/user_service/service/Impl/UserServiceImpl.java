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
            System.out.println("El usuario ya estaba registrado en DB. Retornando existente...");
            return new UserCreationResult(userMapper.toUserResponse(userExistente.get()), false);
        }

        String keycloakId;
        // createdByAdmin=true si el rol NO es ROLE_ALUMNO o si el password no es el oauth dummy
        boolean createdByAdmin = !userRequest.getPassword().equals("oauth_user_password");

        try {
            keycloakId = keycloakService.createUser(userRequest);
        } catch (KeycloakServiceImpl.UserAlreadyExistsInKeycloakException e) {
            System.out.println("Usuario ya existe en Keycloak (OAuth previo). Obteniendo ID y asignando rol...");
            keycloakId = keycloakService.getUserIdByEmail(userRequest.getEmail());
            keycloakService.assignRole(keycloakId, userRequest.getRole());
            createdByAdmin = false;
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
    public void changePassword(String keycloakId, String newPassword) {
        keycloakService.updatePassword(keycloakId, newPassword);
        userRepository.findByKeycloakId(keycloakId).ifPresent(user -> {
            user.setMustChangePassword(false);
            userRepository.save(user);
        });
    }
}