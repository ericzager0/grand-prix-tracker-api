package com.uade.grandprixtracker.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Verifica que ningún error deje llegar detalle interno al cliente.
class GlobalExceptionHandlerTest {

    private static final String DETALLE_INTERNO =
            "ERROR: relation \"clientes\" does not exist; jdbc:postgresql://db.interna:6543/postgres";

    @RestController
    static class ControllerDePrueba {

        @GetMapping("/explota")
        String explota() {
            throw new IllegalStateException(DETALLE_INTERNO);
        }

        @GetMapping("/externo")
        String externo() {
            throw new ServicioExternoException("No se pudo conectar con https://microservicio.interno: " + DETALLE_INTERNO);
        }

        @GetMapping("/con-parametro")
        String conParametro(@RequestParam String requerido) {
            return requerido;
        }

        @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        String json(@RequestBody String body) {
            return body;
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void errorInesperado_devuelve500SinDetalleInterno() throws Exception {
        mockMvc.perform(get("/explota"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.MENSAJE_ERROR_INTERNO))
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("clientes"))));
    }

    @Test
    void servicioExternoCaido_devuelve503ConMensajeGenerico() throws Exception {
        mockMvc.perform(get("/externo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.MENSAJE_SERVICIO_EXTERNO))
                .andExpect(content().string(not(containsString("microservicio.interno"))));
    }

    @Test
    void parametroFaltante_devuelve400YNo500() throws Exception {
        mockMvc.perform(get("/con-parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void contentTypeNoSoportado_devuelve415YNo500() throws Exception {
        mockMvc.perform(post("/json").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void metodoNoPermitido_devuelve405() throws Exception {
        mockMvc.perform(post("/explota"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false));
    }
}
