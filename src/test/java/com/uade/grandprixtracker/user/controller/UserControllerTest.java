package com.uade.grandprixtracker.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
                new BigDecimal("34567890"),
                "#FF8000"
        );

        when(userService.upsertProfile(eq(idCliente), any(UpdateUserProfileRequestDto.class), any()))
                .thenReturn(responseDto);

        String jsonBody = """
                {
                  "nombre": "Lando",
                  "apellido": "Norris",
                  "telefono": "+44 7700 900077",
                  "dni": 34567890,
                  "color": "#FF8000"
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
                .andExpect(jsonPath("$.data.dni").value(34567890))
                .andExpect(jsonPath("$.data.color").value("#FF8000"));
    }

    @Test
    @DisplayName("PUT /profile (ruta alternativa) exitoso con teléfono, DNI y color nulos")
    void testUpdateProfile_AlternativePath_WithNulls() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UserProfileResponseDto responseDto = new UserProfileResponseDto(
                idCliente,
                "Oscar",
                "Piastri",
                "oscar@mclaren.com",
                null,
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
                  "dni": null,
                  "color": null
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
                .andExpect(jsonPath("$.data.dni").doesNotExist())
                .andExpect(jsonPath("$.data.color").doesNotExist());
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

    // Cada campo con un valor fuera de formato o demasiado largo: 400 y el servicio no se llama.
    static java.util.stream.Stream<String> camposInvalidos() {
        return java.util.stream.Stream.of(
                "\"nombre\": \"%s\"".formatted("a".repeat(101)),
                "\"nombre\": \"Lando\", \"apellido\": \"%s\"".formatted("a".repeat(101)),
                "\"nombre\": \"Lando\", \"telefono\": \"<script>alert(1)</script>\"",
                "\"nombre\": \"Lando\", \"telefono\": \"+54 11 1234-5678 9999 0000 1\"",
                "\"nombre\": \"Lando\", \"dni\": -5",
                "\"nombre\": \"Lando\", \"dni\": 1234567890",
                "\"nombre\": \"Lando\", \"dni\": 12.5",
                "\"nombre\": \"Lando\", \"color\": \"red\"",
                "\"nombre\": \"Lando\", \"color\": \"#E10600; background:url(x)\"");
    }

    @ParameterizedTest
    @MethodSource("camposInvalidos")
    @DisplayName("PUT /users/profile con campos fuera de formato o largo retorna 400")
    void testUpdateProfile_CamposInvalidos_Devuelve400(String campos) throws Exception {
        UUID idCliente = UUID.randomUUID();

        mockMvc.perform(put("/users/profile")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + campos + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("PUT /users/profile acepta el formato que manda el front y sin apellido")
    void testUpdateProfile_FormatoDelFront_Devuelve200() throws Exception {
        UUID idCliente = UUID.randomUUID();
        when(userService.upsertProfile(eq(idCliente), any(UpdateUserProfileRequestDto.class), any()))
                .thenReturn(new UserProfileResponseDto(idCliente, "Lando", "", "lando@mclaren.com",
                        "+54 11 1234-5678", new BigDecimal("34567890"), "#E10600"));

        mockMvc.perform(put("/users/profile")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Lando",
                                  "apellido": "",
                                  "telefono": "+54 11 1234-5678",
                                  "dni": 34567890,
                                  "color": "#E10600"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users/profile obtiene el perfil del usuario autenticado con color")
    void testGetProfile_Success() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UserProfileResponseDto responseDto = new UserProfileResponseDto(
                idCliente,
                "Max",
                "Verstappen",
                "max@redbull.com",
                "+31 6 12345678",
                new BigDecimal("11223344"),
                "#0B0B10"
        );

        when(userService.getProfile(idCliente)).thenReturn(responseDto);

        mockMvc.perform(get("/users/profile")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Max"))
                .andExpect(jsonPath("$.data.email").value("max@redbull.com"))
                .andExpect(jsonPath("$.data.color").value("#0B0B10"));
    }
}
