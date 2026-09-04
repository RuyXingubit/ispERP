package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.UsersApi;
import br.dev.xb.isperp.api.dto.UserCreateRequest;
import br.dev.xb.isperp.api.dto.UserResponse;
import br.dev.xb.isperp.api.dto.UserUpdateRequest;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.mapper.UserMapper;
import br.dev.xb.isperp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserController implements UsersApi {

    private final UserService userService;
    private final UserMapper userMapper;

    @Override
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(userMapper.toResponseList(users));
    }

    @Override
    public ResponseEntity<UserResponse> getUserById(UUID id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(u -> ResponseEntity.ok(userMapper.toResponse(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserResponse> createUser(UserCreateRequest userCreateRequest) {
        User entity = userMapper.toEntity(userCreateRequest);
        User created = userService.createUser(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<UserResponse> updateUser(UUID id, UserUpdateRequest userUpdateRequest) {
        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        userMapper.updateEntityFromRequest(userUpdateRequest, user);
        if (userUpdateRequest.getPassword() != null && !userUpdateRequest.getPassword().isBlank()) {
            user.setPassword(userUpdateRequest.getPassword());
        }

        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(userMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
