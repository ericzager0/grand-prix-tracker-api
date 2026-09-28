package com.uade.grandprixtracker.event.soap;

import com.uade.grandprixtracker.event.dto.CircuitoResponseDto;
import com.uade.grandprixtracker.event.dto.CiudadResponseDto;
import com.uade.grandprixtracker.event.dto.EventoF1ResponseDto;
import com.uade.grandprixtracker.event.dto.PaisResponseDto;
import com.uade.grandprixtracker.event.soap.dto.CircuitoSoapDto;
import com.uade.grandprixtracker.event.soap.dto.CiudadSoapDto;
import com.uade.grandprixtracker.event.soap.dto.EventoSoapDto;
import com.uade.grandprixtracker.event.soap.dto.PaisSoapDto;
import org.springframework.stereotype.Component;

@Component
public class EventSoapMapper {

    public EventoSoapDto toSoapDto(EventoF1ResponseDto dto) {
        if (dto == null) {
            return null;
        }

        CircuitoSoapDto circuitoSoap = null;
        if (dto.circuito() != null) {
            CircuitoResponseDto circuito = dto.circuito();
            CiudadSoapDto ciudadSoap = null;

            if (circuito.ciudad() != null) {
                CiudadResponseDto ciudad = circuito.ciudad();
                PaisSoapDto paisSoap = null;

                if (ciudad.pais() != null) {
                    PaisResponseDto pais = ciudad.pais();
                    paisSoap = new PaisSoapDto(
                            pais.idPais() != null ? pais.idPais().toString() : null,
                            pais.nombre(),
                            pais.codigoIso(),
                            pais.continente()
                    );
                }

                ciudadSoap = new CiudadSoapDto(
                        ciudad.idCiudad() != null ? ciudad.idCiudad().toString() : null,
                        ciudad.nombre(),
                        paisSoap
                );
            }

            circuitoSoap = new CircuitoSoapDto(
                    circuito.idCircuito() != null ? circuito.idCircuito().toString() : null,
                    circuito.nombre(),
                    circuito.longitudKm() != null ? circuito.longitudKm().doubleValue() : null,
                    circuito.curvas(),
                    circuito.vueltas(),
                    circuito.mapaSvgUrl(),
                    ciudadSoap
            );
        }

        return new EventoSoapDto(
                dto.idEvento() != null ? dto.idEvento().toString() : null,
                dto.temporada() != null ? dto.temporada() : 0,
                dto.fechaInicio() != null ? dto.fechaInicio().toString() : null,
                dto.fechaFin() != null ? dto.fechaFin().toString() : null,
                dto.estado(),
                circuitoSoap
        );
    }
}
