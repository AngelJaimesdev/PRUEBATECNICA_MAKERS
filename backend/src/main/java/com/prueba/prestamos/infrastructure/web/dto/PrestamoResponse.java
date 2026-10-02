package com.prueba.prestamos.infrastructure.web.dto;

import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.domain.model.Prestamo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrestamoResponse(
        Long id,
        String usuarioEmail,
        BigDecimal monto,
        Integer plazoMeses,
        EstadoPrestamo estado,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaRespuesta
) {
    public static PrestamoResponse from(Prestamo p) {
        return new PrestamoResponse(p.getId(), p.getUsuarioEmail(), p.getMonto(), p.getPlazoMeses(),
                p.getEstado(), p.getFechaSolicitud(), p.getFechaRespuesta());
    }
}
