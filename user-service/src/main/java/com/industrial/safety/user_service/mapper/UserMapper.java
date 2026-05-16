package com.industrial.safety.user_service.mapper;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.dto.UserUpdateRequest;
import com.industrial.safety.user_service.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    User toUser(UserRequest userRequest);
    UserResponse toUserResponse(User user);
    void updateUserFromRequest(UserRequest userRequest, @MappingTarget User user);
    void updateUserFromRequestAdmin(UserUpdateRequest updateRequest, @MappingTarget User user);
}
