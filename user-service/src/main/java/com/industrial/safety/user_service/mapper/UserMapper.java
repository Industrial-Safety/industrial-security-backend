package com.industrial.safety.user_service.mapper;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.model.User;

public interface UserMapper
{
    User toUser(UserRequest userRequest);
    UserResponse toUserResponse(User user);
}
