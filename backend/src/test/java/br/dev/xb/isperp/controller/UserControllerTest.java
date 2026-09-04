package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.UserCreateRequest;
import br.dev.xb.isperp.api.dto.UserResponse;
import br.dev.xb.isperp.api.dto.UserRole;
import br.dev.xb.isperp.api.dto.UserUpdateRequest;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.mapper.UserMapper;
import br.dev.xb.isperp.service.UserService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    private UUID userId;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UuidCreatorUtils.generateUuidV7();

        user = User.builder()
                .id(userId)
                .name("Roberto Silveira (CFO)")
                .email("cfo@nexusfibra.com.br")
                .password("$2a$10$hashedPasswordThatMustNeverLeak")
                .role(br.dev.xb.isperp.entity.UserRole.FINANCIAL)
                .cpf("12345678901")
                .active(true)
                .build();

        userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setName("Roberto Silveira (CFO)");
        userResponse.setEmail("cfo@nexusfibra.com.br");
        userResponse.setRole(UserRole.FINANCIAL);
        userResponse.setCpf("12345678901");
        userResponse.setActive(true);
    }

    @Test
    @DisplayName("GET /users - Deve listar todos os colaboradores")
    void shouldGetAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(user));
        when(userMapper.toResponseList(any())).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].name").value("Roberto Silveira (CFO)"))
                .andExpect(jsonPath("$[0].role").value("FINANCIAL"))
                .andExpect(jsonPath("$[0].password").doesNotExist()); // Garantia de segurança: sem hash

        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("GET /users/{id} - Deve retornar usuário por ID sem expor o hash da senha")
    void shouldGetUserByIdWhenFound() throws Exception {
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        mockMvc.perform(get("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("cfo@nexusfibra.com.br"))
                .andExpect(jsonPath("$.password").doesNotExist()); // Garantia de segurança

        verify(userService).getUserById(userId);
    }

    @Test
    @DisplayName("GET /users/{id} - Deve retornar 404 quando usuário não existir")
    void shouldReturnNotFoundWhenUserNotFound() throws Exception {
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService).getUserById(userId);
    }

    @Test
    @DisplayName("POST /users - Deve cadastrar novo usuário com sucesso")
    void shouldCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Carlos Técnico");
        request.setEmail("carlos.tecnico@nexusfibra.com.br");
        request.setPassword("senhaSegura123");
        request.setRole(UserRole.TECHNICIAN);
        request.setActive(true);

        when(userMapper.toEntity(any(UserCreateRequest.class))).thenReturn(user);
        when(userService.createUser(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(userService).createUser(any(User.class));
    }

    @Test
    @DisplayName("PUT /users/{id} - Deve atualizar usuário existente")
    void shouldUpdateUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Roberto Silveira Junior");
        request.setActive(true);

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userMapper).updateEntityFromRequest(any(UserUpdateRequest.class), eq(user));
        when(userService.updateUser(eq(userId), eq(user))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        mockMvc.perform(put("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));

        verify(userService).updateUser(eq(userId), eq(user));
    }

    @Test
    @DisplayName("PUT /users/{id} - Deve retornar 404 ao atualizar usuário inexistente")
    void shouldReturnNotFoundWhenUpdatingNonExistentUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(put("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{id} - Deve remover usuário com 204 No Content")
    void shouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("DELETE /users/{id} - Deve retornar 404 se usuário não existir")
    void shouldReturnNotFoundWhenDeletingNonExistentUser() throws Exception {
        doThrow(new RuntimeException("Usuário não encontrado")).when(userService).deleteUser(userId);

        mockMvc.perform(delete("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
