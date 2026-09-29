package com.uade.grandprixtracker.auth.handler;

import com.uade.grandprixtracker.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

// 403 con el envoltorio ApiResponse en lugar de la respuesta vacía de Spring Security.
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    static final String MENSAJE = "No tiene permiso para acceder a este recurso";

    // Pone el status y el header WWW-Authenticate estándar de OAuth2 (RFC 6750); el body lo escribimos nosotros.
    private final BearerTokenAccessDeniedHandler delegate = new BearerTokenAccessDeniedHandler();
    private final JsonMapper jsonMapper;

    public ApiAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        delegate.handle(request, response, accessDeniedException);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), ApiResponse.error(MENSAJE));
    }
}
