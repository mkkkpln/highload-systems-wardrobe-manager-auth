package com.example.userservice.controller;

import com.example.userservice.config.SecurityConfig;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.dto.auth.TokenResponseDto;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(properties = {
        "jwt.secret=ZmFrZXNlY3JldGZha2VzZWNyZXRmYWtlc2VjcmV0ZmFrZXNlY3JldA=="
})
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void login_shouldBePublic_andReturnToken() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponseDto.bearer("t", 3600));

        mockMvc.perform(post("/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@example.com","password":"password"}
                                """))
                .andExpect(status().isOk())
                // TokenResponseDto uses snake_case naming
                .andExpect(jsonPath("$.access_token").value("t"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600));
    }

    @Test
    void register_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"x@example.com","name":"Xx","password":"password","role":"ROLE_USER"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void register_shouldReturn403_whenNotSupervisor() throws Exception {
        mockMvc.perform(post("/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"x@example.com","name":"Xx","password":"password","role":"ROLE_USER"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void register_shouldReturn201_whenSupervisor() throws Exception {
        User created = new User();
        created.setId(7L);
        created.setEmail("x@example.com");
        created.setName("X");
        created.setRole(Role.ROLE_USER);

        when(authService.register(any())).thenReturn(created);
        when(userMapper.toDto(created)).thenReturn(new UserResponseDto(7L, "x@example.com", "X"));

        mockMvc.perform(post("/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"x@example.com","name":"Xx","password":"password","role":"ROLE_USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("x@example.com"))
                .andExpect(jsonPath("$.name").value("X"));
    }

    @Test
    void me_shouldReturn401_fromController_whenAuthenticatedButNotJwt() throws Exception {
        // authenticated, but not JwtAuthenticationToken -> controller returns 401 (its own branch)
        mockMvc.perform(get("/users/auth/me").with(user("x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_shouldReturn200_andParseRolesFromCollection() throws Exception {
        mockMvc.perform(get("/users/auth/me").with(jwt().jwt(j -> j
                        .subject("u@example.com")
                        .claim("userId", "123")
                        .claim("roles", List.of("ROLE_USER", "ROLE_SUPERVISOR"))
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("123"))
                .andExpect(jsonPath("$.email").value("u@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void me_shouldReturn200_andHandleRolesNull() throws Exception {
        mockMvc.perform(get("/users/auth/me").with(jwt().jwt(j -> j
                        .subject("u@example.com")
                        .claim("userId", "123")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void me_shouldReturn200_andHandleRolesAsString() throws Exception {
        mockMvc.perform(get("/users/auth/me").with(jwt().jwt(j -> j
                        .subject("u@example.com")
                        .claim("userId", "123")
                        .claim("roles", "ROLE_USER")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }
}


