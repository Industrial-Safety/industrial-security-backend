package com.industrial.safety.user_service.service;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;

import java.util.List;

public interface UserService
{
    UserResponse createUser(UserRequest userRequest);
    List<UserResponse> toListUser();
    UserResponse getUserById(String id);
    UserResponse updateUser(String id, UserRequest userRequest);
}

