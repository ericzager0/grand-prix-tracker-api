package com.uade.grandprixtracker.auth.config;

import com.uade.grandprixtracker.auth.handler.ApiAccessDeniedHandler;
import com.uade.grandprixtracker.auth.handler.ApiAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.json.JsonMapper;

// La identidad sale del JWT de Supabase Auth (claim "sub" = clientes.id_cliente).
// El decoder lo arma Spring Boot con las propiedades spring.security.oauth2.resourceserver.jwt.*.
@Configuration
public class SecurityConfig {

    private static final PathPatternRequestMatcher.Builder PATH = PathPatternRequestMatcher.withDefaults();

    // Endpoints que no dependen del cliente. Todo lo demás (/bookings/**, /payment-methods/**) requiere token.
    private static final RequestMatcher PUBLICOS = new OrRequestMatcher(
            PATH.matcher(HttpMethod.OPTIONS, "/**"),   // preflight de CORS
            PATH.matcher(HttpMethod.GET, "/events/**"), // incluye /events/{id}/hotels, /tickets y /flights
            PATH.matcher("/ws/**"),                     // SOAP
            // Pendiente: las notificaciones todavía reciben ?userId= y el front no les manda token.
            PATH.matcher("/notifications/**"),
            PATH.matcher("/error"));

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(jsonMapper);
        ApiAccessDeniedHandler accessDeniedHandler = new ApiAccessDeniedHandler(jsonMapper);

        http
                // Sin cookies ni sesión: el token viaja en cada request, así que CSRF no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Toma los mappings de CorsConfig (Spring MVC).
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLICOS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    // El front manda el token en todas las requests. En los endpoints públicos se ignora, para que un token
    // vencido no les devuelva 401 (Spring valida cualquier Bearer que encuentre, aunque la ruta sea pública).
    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> PUBLICOS.matches(request) ? null : resolver.resolve(request);
    }
}
