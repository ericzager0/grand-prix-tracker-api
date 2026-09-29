package com.uade.grandprixtracker.auth.handler;

import com.uade.grandprixtracker.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

// 401 con el envoltorio ApiResponse en lugar de la respuesta vacía de Spring Security.
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MENSAJE_SIN_TOKEN = "Se requiere iniciar sesión para acceder a este recurso";
    static final String MENSAJE_TOKEN_INVALIDO = "La sesión es inválida o expiró";

    // Pone el status y el header WWW-Authenticate estándar de OAuth2 (RFC 6750); el body lo escribimos nosotros.
    private final BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
    private final JsonMapper jsonMapper;

    public ApiAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        delegate.commence(request, response, authException);
        String mensaje = authException instanceof OAuth2AuthenticationException
                ? MENSAJE_TOKEN_INVALIDO
                : MENSAJE_SIN_TOKEN;
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), ApiResponse.error(mensaje));
    }
}
