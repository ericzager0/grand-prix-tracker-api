package com.uade.grandprixtracker.user.service;

import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.user.dto.UpdateUserProfileRequestDto;
import com.uade.grandprixtracker.user.dto.UserProfileResponseDto;
import com.uade.grandprixtracker.user.model.Cliente;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final ClienteRepository clienteRepository;

    public UserService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile(UUID idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", idCliente));
        return toDto(cliente);
    }

    @Transactional
    public UserProfileResponseDto upsertProfile(UUID idCliente, UpdateUserProfileRequestDto request, String emailFallback) {
        Cliente cliente = clienteRepository.findById(idCliente).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setIdCliente(idCliente);
            nuevo.setEmail(emailFallback != null && !emailFallback.isBlank() ? emailFallback : idCliente + "@user.supabase");
            return nuevo;
        });

        cliente.setNombre(request.nombre().trim());
        cliente.setApellido(request.apellido().trim());

        if (request.telefono() != null) {
            String tel = request.telefono().trim();
            cliente.setTelefono(tel.isEmpty() ? null : tel);
        } else {
            cliente.setTelefono(null);
        }

        cliente.setDni(request.dni());

        Cliente guardado = clienteRepository.save(cliente);
        return toDto(guardado);
    }

    private UserProfileResponseDto toDto(Cliente cliente) {
        return new UserProfileResponseDto(
                cliente.getIdCliente(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDni()
        );
    }
}
