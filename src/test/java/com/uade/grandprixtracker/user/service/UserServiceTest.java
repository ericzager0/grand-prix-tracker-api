package com.uade.grandprixtracker.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.user.dto.UpdateUserProfileRequestDto;
import com.uade.grandprixtracker.user.dto.UserProfileResponseDto;
import com.uade.grandprixtracker.user.model.Cliente;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(clienteRepository);
    }

    @Test
    @DisplayName("Debe actualizar el perfil de un cliente existente")
    void testUpsertProfile_ExistingCliente_UpdatesFields() {
        UUID idCliente = UUID.randomUUID();
        Cliente existente = new Cliente(idCliente, "Juan", "Perez", "juan@example.com", "+54 11 1111", new BigDecimal("35000000"));

        when(clienteRepository.findById(idCliente)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto(
                "Juan Carlos",
                "Perez Gomez",
                "+54 11 9999-8888",
                new BigDecimal("40123456")
        );

        UserProfileResponseDto resultado = userService.upsertProfile(idCliente, request, "fallback@example.com");

        assertNotNull(resultado);
        assertEquals(idCliente, resultado.idCliente());
        assertEquals("Juan Carlos", resultado.nombre());
        assertEquals("Perez Gomez", resultado.apellido());
        assertEquals("+54 11 9999-8888", resultado.telefono());
        assertEquals(new BigDecimal("40123456"), resultado.dni());
        assertEquals("juan@example.com", resultado.email());
        verify(clienteRepository).save(existente);
    }

    @Test
    @DisplayName("Debe crear un nuevo cliente si no existe previamente (UPSERT)")
    void testUpsertProfile_NewCliente_CreatesAndSaves() {
        UUID idCliente = UUID.randomUUID();
        when(clienteRepository.findById(idCliente)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto(
                "Carlos",
                "Sainz",
                null,
                null
        );

        UserProfileResponseDto resultado = userService.upsertProfile(idCliente, request, "carlos@ferrari.com");

        assertNotNull(resultado);
        assertEquals(idCliente, resultado.idCliente());
        assertEquals("Carlos", resultado.nombre());
        assertEquals("Sainz", resultado.apellido());
        assertNull(resultado.telefono());
        assertNull(resultado.dni());
        assertEquals("carlos@ferrari.com", resultado.email());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe obtener el perfil existente")
    void testGetProfile_Found() {
        UUID idCliente = UUID.randomUUID();
        Cliente existente = new Cliente(idCliente, "Franco", "Colapinto", "franco@williams.com", "+54 11 5555", new BigDecimal("44111222"));
        when(clienteRepository.findById(idCliente)).thenReturn(Optional.of(existente));

        UserProfileResponseDto resultado = userService.getProfile(idCliente);

        assertNotNull(resultado);
        assertEquals("Franco", resultado.nombre());
        assertEquals("Colapinto", resultado.apellido());
        assertEquals(new BigDecimal("44111222"), resultado.dni());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el cliente no existe al consultar perfil")
    void testGetProfile_NotFound_ThrowsException() {
        UUID idCliente = UUID.randomUUID();
        when(clienteRepository.findById(idCliente)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(idCliente));
    }
}
