package com.uade.grandprixtracker.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uade.grandprixtracker.auth.config.SecurityConfig;
import com.uade.grandprixtracker.user.dto.UpdateUserProfileRequestDto;
import com.uade.grandprixtracker.user.dto.UserProfileResponseDto;
import com.uade.grandprixtracker.user.handler.UserHandler;
import com.uade.grandprixtracker.user.service.UserService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, UserHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("PUT /users/profile exitoso con JWT y body completo")
    void testUpdateProfile_Success() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UserProfileResponseDto responseDto = new UserProfileResponseDto(
                idCliente,
                "Lando",
                "Norris",
                "lando@mclaren.com",
                "+44 7700 900077",
                new BigDecimal("34567890")
        );

        when(userService.upsertProfile(eq(idCliente), any(UpdateUserProfileRequestDto.class), any()))
                .thenReturn(responseDto);

        String jsonBody = """
                {
                  "nombre": "Lando",
                  "apellido": "Norris",
                  "telefono": "+44 7700 900077",
                  "dni": 34567890
                }
                """;

        mockMvc.perform(put("/users/profile")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString()).claim("email", "lando@mclaren.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Lando"))
                .andExpect(jsonPath("$.data.apellido").value("Norris"))
                .andExpect(jsonPath("$.data.telefono").value("+44 7700 900077"))
                .andExpect(jsonPath("$.data.dni").value(34567890));
    }

    @Test
    @DisplayName("PUT /profile (ruta alternativa) exitoso con teléfono y DNI nulos")
    void testUpdateProfile_AlternativePath_WithNulls() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UserProfileResponseDto responseDto = new UserProfileResponseDto(
                idCliente,
                "Oscar",
                "Piastri",
                "oscar@mclaren.com",
                null,
                null
        );

        when(userService.upsertProfile(eq(idCliente), any(UpdateUserProfileRequestDto.class), any()))
                .thenReturn(responseDto);

        String jsonBody = """
                {
                  "nombre": "Oscar",
                  "apellido": "Piastri",
                  "telefono": null,
                  "dni": null
                }
                """;

        mockMvc.perform(put("/profile")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString()).claim("email", "oscar@mclaren.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Oscar"))
                .andExpect(jsonPath("$.data.apellido").value("Piastri"))
                .andExpect(jsonPath("$.data.telefono").doesNotExist())
                .andExpect(jsonPath("$.data.dni").doesNotExist());
    }

    @Test
    @DisplayName("PUT /users/profile sin JWT retorna 401 Unauthorized")
    void testUpdateProfile_Unauthorized() throws Exception {
        String jsonBody = """
                {
                  "nombre": "Lando",
                  "apellido": "Norris"
                }
                """;

        mockMvc.perform(put("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /users/profile con nombre vacío retorna 400 Bad Request")
    void testUpdateProfile_ValidationError() throws Exception {
        UUID idCliente = UUID.randomUUID();
        String jsonBody = """
                {
                  "nombre": "",
                  "apellido": "Norris"
                }
                """;

        mockMvc.perform(put("/users/profile")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /users/profile obtiene el perfil del usuario autenticado")
    void testGetProfile_Success() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UserProfileResponseDto responseDto = new UserProfileResponseDto(
                idCliente,
                "Max",
                "Verstappen",
                "max@redbull.com",
                "+31 6 12345678",
                new BigDecimal("11223344")
        );

        when(userService.getProfile(idCliente)).thenReturn(responseDto);

        mockMvc.perform(get("/users/profile")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Max"))
                .andExpect(jsonPath("$.data.email").value("max@redbull.com"));
    }
}
