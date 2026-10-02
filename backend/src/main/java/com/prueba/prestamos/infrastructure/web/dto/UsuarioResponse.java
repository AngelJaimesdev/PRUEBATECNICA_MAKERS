package com.prueba.prestamos.infrastructure.web.dto;

import com.prueba.prestamos.domain.model.Rol;
import com.prueba.prestamos.domain.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String email, Rol rol) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
    }
}
