package com.industrial.safety.user_service.service;

import com.industrial.safety.user_service.dto.UserCreationResult;
import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.dto.UserUpdateRequest;

import java.util.List;

public interface UserService
{
    UserCreationResult createUser(UserRequest userRequest);
    UserResponse createUserAdmin(UserRequest userRequest);
    List<UserResponse> toListUser();
    UserResponse getUserById(String id);
    UserResponse getUserByEmail(String email);
    UserResponse updateUser(String id, UserRequest userRequest);
    UserResponse updateUserAdmin(String id, UserUpdateRequest userUpdateRequest);
    void changePassword(String keycloakId, String email, String newPassword);
    UserResponse toggleStatus(String id);
}