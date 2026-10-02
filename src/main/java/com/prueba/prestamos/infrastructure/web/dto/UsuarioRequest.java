package com.prueba.prestamos.infrastructure.web.dto;

import com.prueba.prestamos.domain.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @Size(min = 3, max = 72, message = "La contraseña debe tener entre 3 y 72 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol
) {
}
