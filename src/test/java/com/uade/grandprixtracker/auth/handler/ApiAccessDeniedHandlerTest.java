package com.uade.grandprixtracker.auth.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Hoy ninguna regla devuelve 403 (solo se exige estar autenticado), así que el handler se prueba directo.
class ApiAccessDeniedHandlerTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void handle_Devuelve403ConApiResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ApiAccessDeniedHandler(jsonMapper)
                .handle(new MockHttpServletRequest(), response, new AccessDeniedException("denegado"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertEquals(false, body.get("success").asBoolean());
        assertEquals(ApiAccessDeniedHandler.MENSAJE, body.get("message").asString());
        assertTrue(body.get("data").isNull());
    }
}
