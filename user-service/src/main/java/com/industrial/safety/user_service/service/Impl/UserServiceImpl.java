package com.industrial.safety.user_service.service.Impl;

import com.industrial.safety.user_service.dto.UserRequest;
import com.industrial.safety.user_service.dto.UserResponse;
import com.industrial.safety.user_service.exception.ResourceNotFoundException;
import com.industrial.safety.user_service.mapper.UserMapper;
import com.industrial.safety.user_service.model.User;
import com.industrial.safety.user_service.repository.UserRepository;
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

    @Override
    public UserResponse createUser(UserRequest userRequest) {
        User user = userMapper.toUser(userRequest);
        user.setIsActive(true);
        user.setCreateAccount(LocalDate.now());
        user.setQrCodeUrl(qrService.generateAndUploadQr());
        User nUser = userRepository.save(user);
        return userMapper.toUserResponse(nUser);
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
                ()->   new ResourceNotFoundException("No existe el id ", "id",id)
        );
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(String id, UserRequest userRequest) {
        User user = userRepository.findById(id).orElseThrow(
                ()->   new ResourceNotFoundException("No existe el id ", "id",id)
        );
        userMapper.updateUserFromRequest(userRequest,user);
        User userUpdate = userRepository.save(user);
        return userMapper.toUserResponse(userUpdate);

    }
}
