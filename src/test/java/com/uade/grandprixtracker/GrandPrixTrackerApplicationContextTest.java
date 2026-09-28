package com.uade.grandprixtracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Levanta el contexto sin conexión a la base: valida mapeos JPA y consultas @Query de los repositorios.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:1/sin-base",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
class GrandPrixTrackerApplicationContextTest {

    @Test
    void contextLoads() {
    }
}
