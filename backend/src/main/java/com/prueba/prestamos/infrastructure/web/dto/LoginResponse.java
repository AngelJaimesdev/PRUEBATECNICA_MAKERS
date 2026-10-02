package com.prueba.prestamos.infrastructure.web.dto;

import com.prueba.prestamos.domain.model.Rol;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String nombre,
        String email,
        Rol rol
) {
}
