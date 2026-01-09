package com.example.userservice.controller;

import com.example.userservice.config.SecurityConfig;
import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(properties = {
        "jwt.secret=ZmFrZXNlY3JldGZha2VzZWNyZXRmYWtlc2VjcmV0ZmFrZXNlY3JldA=="
})
class UserControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getAll_shouldReturn403_forRegularUser() throws Exception {
        mockMvc.perform(get("/users").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("userId", "10").subject("u@example.com"))
                ))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_shouldReturn200_forSupervisor() throws Exception {
        when(userService.getAll()).thenReturn(List.of(new UserResponseDto(1L, "a@a", "A")));

        mockMvc.perform(get("/users").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))
                        .jwt(j -> j.claim("userId", "1").subject("s@example.com"))
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(userService).getAll();
    }

    @Test
    void getById_shouldAllowSelfAccess_forUser() throws Exception {
        when(userService.getById(10L)).thenReturn(new UserResponseDto(10L, "u@u", "U"));

        mockMvc.perform(get("/users/10").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("userId", "10").subject("u@u"))
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getById_shouldReturn403_whenUserAccessingOtherUser() throws Exception {
        mockMvc.perform(get("/users/11").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("userId", "10").subject("u@u"))
                ))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldAllowSupervisor() throws Exception {
        when(userService.getById(11L)).thenReturn(new UserResponseDto(11L, "x@x", "X"));

        mockMvc.perform(get("/users/11").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))
                        .jwt(j -> j.claim("userId", "1").subject("s@s"))
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void getById_shouldAllowModerator_forOtherUser() throws Exception {
        when(userService.getById(11L)).thenReturn(new UserResponseDto(11L, "x@x", "X"));

        mockMvc.perform(get("/users/11").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"))
                        .jwt(j -> j.claim("userId", "102").subject("moderator@example.com"))
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void create_shouldReturn201_forSupervisor() throws Exception {
        when(userService.create(any(UserDto.class))).thenReturn(new UserResponseDto(5L, "n@n", "N"));

        mockMvc.perform(post("/users").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))
                        .jwt(j -> j.claim("userId", "1").subject("s@s"))
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"n@n","name":"NN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void update_shouldAllowSelfAccess_forUser() throws Exception {
        when(userService.update(eq(10L), any(UserDto.class))).thenReturn(new UserResponseDto(10L, "u@u", "Updated"));

        mockMvc.perform(put("/users/10").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("userId", "10").subject("u@u"))
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@u","name":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void delete_shouldAllowSelfAccess_forUser() throws Exception {
        mockMvc.perform(delete("/users/10").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("userId", "10").subject("u@u"))
                ))
                .andExpect(status().isNoContent());

        verify(userService).delete(10L);
    }
}


