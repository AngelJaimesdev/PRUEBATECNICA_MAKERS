package com.prueba.prestamos.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class Usuario {

    private Long id;
    private String nombre;
    private String email;
    private String passwordHash;
    private Rol rol;

    public void actualizarDatos(String nombre, String email, Rol rol) {
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    public void cambiarPassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
